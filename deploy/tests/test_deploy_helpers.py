import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from config_values import values
from prune_releases import prune


class ConfigTests(unittest.TestCase):
    def config(self):
        return {'services': {'backend': {'environment': {
            'DB_URL': 'jdbc:mysql://ocasion_db_dom2:3306/ocasion_salon_db',
            'DB_USERNAME': 'app', 'DB_PASSWORD': 'secreto-simulado', 'JWT_SECRET': 'jwt-simulado',
            'DOM2_DB_CONTAINER': 'ocasion_db_dom2'},
            'ports': [{'host_ip':'172.30.219.232', 'published':'8083'}]}},
            'networks': {'base_existente': {'name':'ocasion-dom2-net'}}}

    def test_solo_devuelve_metadatos_sin_secretos(self):
        result = values(self.config())
        self.assertEqual(result, ('172.30.219.232', '8083', 'ocasion-dom2-net', 'ocasion_db_dom2'))
        self.assertNotIn('secreto-simulado', repr(result))

    def test_rechaza_plantillas_incompletas_y_publicacion_global(self):
        for key in ('DB_URL','DB_USERNAME','DB_PASSWORD','JWT_SECRET'):
            config=self.config(); config['services']['backend']['environment'][key]='REEMPLAZAR'
            with self.assertRaisesRegex(ValueError, key): values(config)
        config=self.config(); config['services']['backend']['ports'][0]['host_ip']='0.0.0.0'
        with self.assertRaises(ValueError): values(config)

    def test_no_acepta_nombres_que_sean_rutas(self):
        config=self.config(); config['networks']['base_existente']['name']='../mysql'
        with self.assertRaises(ValueError): values(config)


class CleanupTests(unittest.TestCase):
    def release(self, root, n):
        directory=root/'releases'/f'release-{n}'; directory.mkdir(parents=True)
        (directory/'.dom2-release').touch()
        (directory/'release.json').write_text(json.dumps({'image':f'ocasion-dom2:test-{n}'}))
        os.utime(directory,(1000+n,1000+n))
        return directory

    def test_conserva_tres_releases_y_no_toca_directorios_ajenos_ni_volumenes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp); directories=[self.release(root,n) for n in range(5)]
            ajeno=root/'releases'/'mysql'; ajeno.mkdir(); (ajeno/'datos').write_text('conservar')
            with patch('prune_releases.subprocess.run', return_value=subprocess.CompletedProcess([],0)) as docker:
                prune(root)
            self.assertEqual([p.exists() for p in directories],[False,False,True,True,True])
            self.assertTrue((ajeno/'datos').exists())
            self.assertEqual([call.args[0] for call in docker.call_args_list],
                [['docker','image','rm','ocasion-dom2:test-1'], ['docker','image','rm','ocasion-dom2:test-0']])

    def test_no_borra_release_si_docker_conserva_imagen_en_uso(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp); directories=[self.release(root,n) for n in range(4)]
            with patch('prune_releases.subprocess.run', return_value=subprocess.CompletedProcess([],1)):
                prune(root)
            self.assertTrue(directories[0].exists())

    def test_protege_la_version_activa_aunque_no_sea_de_las_tres_recientes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp); directories=[self.release(root,n) for n in range(5)]
            try: (root/'current').symlink_to(directories[0],target_is_directory=True)
            except OSError: self.skipTest('Crear symlinks requiere permiso del sistema en Windows.')
            with patch('prune_releases.subprocess.run', return_value=subprocess.CompletedProcess([],0)):
                prune(root)
            self.assertTrue(directories[0].exists())


if __name__ == '__main__': unittest.main()
