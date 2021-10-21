# Shmexel Spreadsheet
## Overview
A spreadsheet is a tool with table to store and analyze data. \
A cell may be represented by: 
* number value
* reference to another cell
* arithmetic expression
* operation on a range of cells
<!-- end of the list -->
A spreadsheet dynamically evaluates value in each cell. 
## Window view and functionality
Before the initial spreadsheet appears, the input pane for amount of rows and columns is displayed.
![Set rows and columns](resources/markdown/rows_cols.png) \
The main window contains a table, upper text field to get input from user and to \
display formula of selected cell, as well as the following buttons:
* undo 
* reset
* save 
* load 
<!-- end of the list -->
![spreadsheet.Main window](resources/markdown/spreadsheet.png) \
Highlight cells in the sheet when selecting its reference in the input text pane. \
![Highlight reference from formula](highlight.gif) \
Handle wrong input. \
![Wrong input -> error](ref_val.gif) \
Reset and undo. \
![Reset and undo](reset_undo.gif) \
Save and load file. \
<img src="save_load.gif" width="500" height="500"/>


## Supported functions and arithmetic operations
* Addition '+'
* Subtraction '-'
* Multiplication '*'
* Division '/'
* Power '^'
* Square root 'sqrt(NUM, <span id="a1">[EXPR](#f1)</span> or <span id="a2">[REF](#f2)</span>)' 
* SUM(REF1:REF2;REF3;REF4:REF5;...)
* AVERAGE(REF1:REF2;REF3;REF4:REF5;...)
* MIN(REF1:REF2;REF3;REF4:REF5;...)
* MAX(REF1:REF2;REF3;REF4:REF5;...) \
![Functions](func.gif)
## Run and build
Clone repository and run Shmexel_Spreadsheet-1.0.jar file. 

### Footnotes
1. <span id="f1"></span> Expression is an equals sign, followed by numbers, references and allowed operators, i.e. =2+2 or =A1*4.
2. <span id="f2"></span> Reference is a latin letter, followed by number.






