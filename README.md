<h1>Person Action Object</h1>

This project is used to help me quiz my personal 6-digit [Person-Action-Object](https://artofmemory.com/blog/pao-system/) system.
My master list of PAO entries are stored externally, copied into this program when updated.

This command line utility offers the following functionality:
* Quiz that cycles through numbers and gives me something about that entry (ie number, person, action, object), asking me to answer another aspect of that entry
* Quiz asking for the mental image description of a 6-digit number
    * For example, "202122" would be "Luke Skywalker making robot beeping while running around the millenium falcon"  (20 is Luke, 21 is R2-D2, 22 is Han)
* Help entry with description of all commands
* On program run, update source data file in pretty csv format
* Can set start and end entry numbers for quiz
* Flexible answer checker
    * Entries can have alternate acceptable values (ie entry 45's action can be "steeping tea", "drinking tea", "sipping tea", or "making tea")
    * If answer is close but not exactly correct, mark as correct but print exact answer
    * Case insensitive
