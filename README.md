
![alt text](http://abload.de/img/mazegenlogon0by1.jpg "MazeGen Logo")


# MazeGen

Kotlin maze generator console application. 

This was a private weekend project just for learning Kotlin. The code is ugly, dirty, messy, ... you name it. However it was fun to learn Kotlin and in the same moment create a "useful" (well... more useful than an ordinary exercise programm) programm in a hackathonish way. So the code formatting, code style in general, comments, "architecture" and so on are everything else than impressive.

* Open `maze.html` with your browser to see a generated maze
* I believe that the exit field can be blocked by walls right on this field, consider it a bug
* There are two "printers", a HTML printer (currently active) and a text file / console / ASCII art printer that might have at least one bug
* Both "printers" will create a file to save the maze
* They will also spam your console with a printed version of the maze everytime something in the maze has changed
* The HTML file has two feature: Mouseover that displays the current positon (row/cow) and you can mark fields by clicking) on them
* There is a lot of unused code in the .kt file
