from pathlib import Path
import sys
import xml.etree.ElementTree as ET

reports = list(Path(sys.argv[1]).glob('TEST-*.xml'))
totals = dict.fromkeys(('tests', 'failures', 'errors', 'skipped'), 0)
for report in reports:
    root = ET.parse(report).getroot()
    for key in totals:
        totals[key] += int(root.get(key, 0))
print('### Pruebas de DOM2')
if not reports:
    print('No se generaron reportes; revisa el paso de compilación.')
else:
    print(f"- Casos: {totals['tests']}\n- Aprobados: {totals['tests']-totals['failures']-totals['errors']-totals['skipped']}")
    print(f"- Fallos: {totals['failures']}\n- Errores: {totals['errors']}\n- Omitidos: {totals['skipped']}")
