Java VM has 64k method bytecode limit, so Babel need to be split to fit into this limit.

Steps to setup Babel for CocoDoc
================================

1. Download http://babeljs.io/scripts/babel.js

2. Format the file with http://jsbeautifier.org/ and removes *all* blank lines.

3. Save file as `babel.formatted.js` (unix line ending).

4. The file has two sections:
  4.1 A main function that detects global and loads all modules. As of writing, this part ends with "[31])(31)" (Number may varies)
  4.2 Core-js and regenerator loader, about 3400 lines (use bracket match to find the start/end).

5. Add a blank line after the last babel module, i.e. before the line "}, {}, [31])(31)".

6. Add a blank line before the core-js function, i.e. after  the line "}, {}, [31])(31)});".

7. Copy the babel loader, about 44 lines, into `babel.loader.js`.  Replaces the modules with a variable named `modules`.

8. Pray that the code format didn't change (much).  CocoDoc will read the formatted code and try to load individual modules.
   If it did change, you may need to update TaskJS's loading routines.