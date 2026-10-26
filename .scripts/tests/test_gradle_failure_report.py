import importlib.util
from pathlib import Path
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'report-gradle-test-failures.py'
SPEC = importlib.util.spec_from_file_location('failure_report', SCRIPT)
REPORT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(REPORT)


class GradleFailureReportTest(unittest.TestCase):
    def test_reports_failure_and_error_but_not_passing_tests(self):
        # Missing either XML kind would hide the actual CI failure.
        scratch = Path(__file__).resolve().parents[2] / '.tmp/failure-report-tests'
        scratch.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=scratch) as directory:
            root = Path(directory)
            output = root / 'desktopApp/build/test-results/allTests'
            output.mkdir(parents=True)
            (output / 'TEST-window.xml').write_text(
                '<testsuite><testcase classname="WindowTest" name="focus">'
                '<failure message="not focused">Assertion at WindowTest:42</failure></testcase>'
                '<testcase classname="OtherTest" name="crash"><error>Native error</error></testcase>'
                '<testcase classname="PassingTest" name="okay"/></testsuite>', encoding='utf-8',
            )
            result = REPORT.failures(root)
            self.assertEqual(len(result), 2)
            self.assertIn('WindowTest.focus', result[0])
            self.assertIn('Assertion at WindowTest:42', result[0])
            self.assertIn('OtherTest.crash', result[1])
            self.assertIn('Native error', result[1])


if __name__ == '__main__':
    unittest.main()
