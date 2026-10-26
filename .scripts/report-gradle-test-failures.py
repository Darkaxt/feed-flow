"""Expose JUnit failure details even when Gradle runs with --quiet."""
from pathlib import Path
import xml.etree.ElementTree as ET


def failures(root: Path) -> list[str]:
    result = []
    for path in sorted(root.glob('**/build/test-results/**/TEST-*.xml')):
        for case in ET.parse(path).getroot().iter('testcase'):
            for problem in case:
                if problem.tag in ('failure', 'error'):
                    result.append(
                        f"{path.relative_to(root)}: {case.get('classname')}.{case.get('name')}\n"
                        f"{problem.get('message', '')}\n{problem.text or ''}",
                    )
    return result


if __name__ == '__main__':
    print('\n'.join(failures(Path('.'))))
