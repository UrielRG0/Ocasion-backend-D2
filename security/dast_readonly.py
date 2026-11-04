"""Comprobaciones HTTP limitadas; no escanea rutas ni modifica registros."""
import argparse
import json
from datetime import datetime, timezone
from urllib.request import Request, build_opener, HTTPRedirectHandler
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dom2', default='http://172.30.120.90:8081')
    parser.add_argument('--front', default='http://172.30.219.232:8082')
    parser.add_argument('--output', default='dast-resultados.json')
    args = parser.parse_args()
    for url in [args.dom2, args.front]:
        p = urlparse(url)
        if p.scheme not in ('http', 'https') or p.username or p.password or p.query or p.fragment or p.path not in ('', '/'):
            parser.error('Usa únicamente el origen autorizado, sin credenciales ni rutas.')
    results = []
    opener = build_opener(NoRedirect())
    def check(name, url, expected, headers=None, method='GET', cors=False):
        try:
            try:
                response = opener.open(Request(url, headers=headers or {}, method=method), timeout=5)
            except HTTPError as exc:
                response = exc
            with response:
                status = response.code
                h = response.headers
                ok = status in expected and (not cors or not h.get('Access-Control-Allow-Origin'))
                results.append(dict(prueba=name, estado='PASA' if ok else 'REVISAR', http=status,
                    cabeceras={k: h.get(k) for k in ['X-Content-Type-Options','Content-Security-Policy','Strict-Transport-Security','Cache-Control','Access-Control-Allow-Origin']}))
        except (URLError, TimeoutError, OSError):
            results.append(dict(prueba=name, estado='NO_EJECUTADA', motivo='Destino inaccesible o timeout; no es una prueba aprobada.'))
    api=args.dom2.rstrip('/')
    check('Control positivo: catálogo público disponible',api+'/api/v1/salones/explorar',{200})
    check('Lista privada sin autenticación',api+'/api/v1/salones/propietario',{401,403})
    check('Administración sin autenticación',api+'/api/v1/admin/salones/pendientes',{401,403})
    check('JWT malformado rechazado',api+'/api/v1/salones/propietario',{401,403},{'Authorization':'Bearer invalid.token.value'})
    check('CORS rechaza origen ajeno',api+'/api/v1/salones/propietario',{403},
          {'Origin':'https://untrusted.invalid','Access-Control-Request-Method':'GET'},'OPTIONS',True)
    check('Front disponible; cabeceras para revisión manual',args.front.rstrip('/')+'/',{200})
    report=dict(fecha=datetime.now(timezone.utc).isoformat(),alcance='GET y OPTIONS; sin autenticación real, sin escritura, sin seguir redirecciones. No equivale a un pentest completo.',
        dom2=args.dom2,front=args.front,resultados=results)
    with open(args.output,'w',encoding='utf-8') as f: json.dump(report,f,ensure_ascii=False,indent=2)
    for result in results: print(result['estado']+': '+result['prueba'])
    return 1 if any(x['estado']!='PASA' for x in results) else 0

if __name__=='__main__':
    raise SystemExit(main())
