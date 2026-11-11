"""Mide GET de Ocasión sin guardar cuerpos, documentos ni credenciales."""
import argparse
import json
import os
import time
from datetime import datetime, timezone
from urllib.request import Request, build_opener, HTTPRedirectHandler
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse

class SinRedireccion(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--base', default='http://172.30.120.90:8081')
    parser.add_argument('--repeticiones', type=int, default=2)
    parser.add_argument('--timeout', type=float, default=15)
    parser.add_argument('--output', default='medicion-lecturas.json')
    args = parser.parse_args()
    origin = urlparse(args.base)
    if origin.scheme not in ('http','https') or origin.username or origin.password or origin.path not in ('','/') or origin.query or origin.fragment:
        parser.error('Usa el origen de tu backend, sin ruta ni credenciales.')
    if not 1 <= args.repeticiones <= 5 or not 1 <= args.timeout <= 60:
        parser.error('Usa 1 a 5 repeticiones y timeout de 1 a 60 segundos.')
    targets = [('servicios','/api/v1/servicios',None),('catalogo','/api/v1/salones/explorar?page=0&size=12',None)]
    for name,path,env in [('vendedor','/api/v1/salones/propietario','OCASION_TOKEN_VENDEDOR'),
                          ('admin','/api/v1/admin/salones/pendientes?page=0&size=10','OCASION_TOKEN_ADMIN')]:
        token = os.environ.get(env)
        if token:
            targets.append((name,path,token))
        else:
            print(name+': omitido; falta variable '+env+' (no compartir el token).')
    opener = build_opener(SinRedireccion())
    results = []
    for name,path,token in targets:
        for sample in range(args.repeticiones):
            headers = {'Accept':'application/json','Accept-Encoding':'identity'}
            if token: headers['Authorization']='Bearer '+token
            start = time.perf_counter()
            row = dict(ruta=path,muestra=sample+1)
            try:
                try:
                    response = opener.open(Request(args.base.rstrip('/')+path,headers=headers),timeout=args.timeout)
                except HTTPError as exc:
                    response = exc
                with response:
                    row['http'] = response.code
                    row['hasta_cabeceras_s'] = round(time.perf_counter()-start,4)
                    size=0
                    while chunk := response.read(64*1024): size+=len(chunk)
                    row['bytes'] = size
                    row['estado'] = 'COMPLETADA' if response.code==200 else 'HTTP_NO_EXITOSO'
            except (URLError,TimeoutError,OSError):
                row['estado']='SIN_COMPLETAR'
            row['total_s']=round(time.perf_counter()-start,4)
            results.append(row)
            print(name+': '+json.dumps(row),flush=True)
    with open(args.output,'w',encoding='utf-8') as report:
        json.dump(dict(fecha=datetime.now(timezone.utc).isoformat(),origen=args.base,
            nota='Mediciones desde este equipo; no separan CPU, SQL y red. No se guardan cuerpos ni tokens.',
            resultados=results),report,ensure_ascii=False,indent=2)
    return 1 if any(x['estado']!='COMPLETADA' for x in results) else 0

if __name__=='__main__':
    raise SystemExit(main())
