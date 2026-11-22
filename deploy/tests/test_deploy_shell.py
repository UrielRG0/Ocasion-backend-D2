"""Prueba Linux del script completo: Docker/IP/curl simulados, sin servidor real."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'deploy-dom2.sh'
FAKE_DOCKER = r'''#!/usr/bin/env python3
import json,os,sys
args=sys.argv[1:]
with open(os.environ['TEST_DOCKER_LOG'],'a') as log:
    log.write(json.dumps({'args':args,'image':os.environ.get('DOM2_IMAGE')})+'\n')
if args[0]=='compose' and 'version' in args:
    sys.exit(1 if os.environ.get('TEST_NO_COMPOSE')=='1' else 0)
elif args[0]=='compose' and 'config' in args:
    if os.environ.get('TEST_BAD_CONFIG')=='1': sys.exit(1)
    print(os.environ['TEST_COMPOSE_CONFIG'])
elif args[0]=='compose' and 'up' in args:
    if os.environ.get('TEST_FAIL_NEW')=='1' and os.environ.get('DOM2_IMAGE')!='ocasion-dom2:previous': sys.exit(1)
elif args[0]=='inspect':
    if args[-1]=='ocasion_db_dom2':
        print(json.dumps({'ocasion-dom2-net':{}}) if 'Networks' in ' '.join(args) else 'true')
    elif args[-1]=='ocasion-dom2':
        if os.environ.get('TEST_EXISTING')!='1': sys.exit(1)
        if '.Config.Image' in ' '.join(args): print('ocasion-dom2:previous')
        elif '.Config.Labels' in ' '.join(args): print(os.environ.get('TEST_PROJECT','ocasion-dom2'))
'''


@unittest.skipUnless(sys.platform != 'win32' and shutil.which('bash'), 'Se ejecuta con Bash real en el runner Linux de CI.')
class DeployShellTests(unittest.TestCase):
    def run_deploy(self, tmp, existing=False, fail=False, project='ocasion-dom2', no_compose=False, bad_config=False):
        from test_deploy_helpers import ConfigTests
        root=Path(tmp); state=root/'state'; state.mkdir(); fake=root/'bin'; fake.mkdir()
        artifact=root/'artifact'; artifact.mkdir(); (artifact/'app.jar').write_bytes(b'jar-simulado')
        env_file=state/'.env'; env_file.write_text('configuracion privada simulada\n'); env_file.chmod(0o600)
        for name, script in [('docker',FAKE_DOCKER),
            ('ip', '#!/usr/bin/env python3\nprint("3: zt0 inet 172.30.219.232/16 brd 172.30.255.255")\n'),
            ('curl','#!/usr/bin/env python3\nprint(\'{"status":"UP"}\')\n')]:
            file=fake/name; file.write_text(script); file.chmod(0o755)
        previous=state/'releases'/'previous'
        if existing:
            previous.mkdir(parents=True); (previous/'compose.yml').write_text('services: {}\n')
            (state/'current').symlink_to(previous,target_is_directory=True)
        env=os.environ.copy(); env.update({
            'PATH':str(fake)+os.pathsep+env['PATH'], 'DOM2_STATE_DIR':str(state),
            'TEST_DOCKER_LOG':str(root/'docker.log'), 'TEST_COMPOSE_CONFIG':json.dumps(ConfigTests().config()),
            'TEST_EXISTING':'1' if existing else '0', 'TEST_FAIL_NEW':'1' if fail else '0', 'TEST_PROJECT':project,
            'TEST_NO_COMPOSE':'1' if no_compose else '0', 'TEST_BAD_CONFIG':'1' if bad_config else '0'})
        result=subprocess.run(['bash',str(SCRIPT),str(artifact)],env=env,text=True,capture_output=True,timeout=30)
        calls=[json.loads(line) for line in (root/'docker.log').read_text().splitlines()]
        self.assertNotIn('secreto-simulado',result.stdout+result.stderr)
        self.assertFalse(any('--volumes' in call['args'] or 'prune' in call['args'] for call in calls))
        return result,calls,state,previous

    def test_sin_compose_se_detiene_antes_de_leer_configuracion(self):
        with tempfile.TemporaryDirectory() as tmp:
            result,calls,_,_=self.run_deploy(tmp,no_compose=True)
            self.assertNotEqual(result.returncode,0)
            self.assertIn('Falta el complemento Docker Compose',result.stderr)
            self.assertEqual(len(calls),1)

    def test_error_compose_no_provoca_error_json_secundario(self):
        with tempfile.TemporaryDirectory() as tmp:
            result,calls,_,_=self.run_deploy(tmp,bad_config=True)
            self.assertNotEqual(result.returncode,0)
            self.assertIn('no pudo validar',result.stderr)
            self.assertNotIn('Expecting value',result.stderr)
            self.assertFalse(any('build' in call['args'] or 'up' in call['args'] for call in calls))

    def test_publica_solo_backend_y_registra_release(self):
        with tempfile.TemporaryDirectory() as tmp:
            result,calls,state,_=self.run_deploy(tmp)
            self.assertEqual(result.returncode,0,result.stderr)
            self.assertTrue((state/'current').is_symlink())
            ups=[call for call in calls if 'up' in call['args']]
            self.assertEqual(len(ups),1); self.assertEqual(ups[0]['args'][-1],'backend')
            self.assertFalse((state/'current'/'context'/'.deploy-artifact'/'app.jar').exists())

    def test_un_fallo_restaura_la_imagen_anterior_sin_tocar_mysql(self):
        with tempfile.TemporaryDirectory() as tmp:
            result,calls,state,previous=self.run_deploy(tmp,existing=True,fail=True)
            self.assertEqual(result.returncode,1)
            ups=[call for call in calls if 'up' in call['args']]
            self.assertEqual(len(ups),2)
            self.assertEqual(ups[-1]['image'],'ocasion-dom2:previous')
            self.assertEqual((state/'current').resolve(),previous)

    def test_primera_instalacion_fallida_retira_solo_backend(self):
        with tempfile.TemporaryDirectory() as tmp:
            result,calls,state,_=self.run_deploy(tmp,fail=True)
            self.assertEqual(result.returncode,1)
            removals=[call for call in calls if call['args'][0]=='compose' and 'rm' in call['args']]
            self.assertEqual(len(removals),1); self.assertEqual(removals[0]['args'][-1],'backend')
            self.assertFalse((state/'current').exists())

    def test_no_reemplaza_contenedores_de_otro_proyecto(self):
        with tempfile.TemporaryDirectory() as tmp:
            result,calls,_,_=self.run_deploy(tmp,existing=True,project='otro-proyecto')
            self.assertEqual(result.returncode,1)
            self.assertFalse(any('up' in call['args'] for call in calls))
