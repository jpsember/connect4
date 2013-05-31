Connect4
===========

Connect4 is a Java applet that I wrote years ago.  It appears here as an Eclipse project.

Loading the page "src/Connect4.html" within a browser should (maybe) start the program.
Failing that, from a command line you can try "appletviewer src/Connect4.html".

An interesting feature of the game is that while you can instruct the computer not to think 
too deeply during its turn (e.g., to keep the game moving along quickly), if the 'think ahead'
option is selected, the computer can use the time that you are thinking of your move to 
1) anticipate the three or so most likely moves you will make, and 2) for each of them, search 
for the best possible response.  Thus, if you do make one of the anticipated moves, it can respond
immediately with its move.  What's more, if you've spent significant time thinking of your move,
the computer has had time to look very deeply into the game tree to pick its best response.  
This can lead to the somewhat surprising message that the computer will 'win in ten moves' even 
if its lookahead is only set to two or three.

