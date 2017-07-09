import java.io.File
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

fun main(args: Array<String>) {
    val mazeGen = MazeGen(45, 31) // Small maze
    //val mazeGen = MazeGen1(79, 59) // Big maze

    val printer : MazePrinter

    val useHtmlPrinter = true
    if (useHtmlPrinter) {
        printer = HtmlPrinter(mazeGen)
    } else {
        printer = ConsolePrinter(mazeGen)
    }

    mazeGen.generate()
    var gatePositions = mazeGen.solvable()

    //while(gatePositions.size > 0) {
    //    mazeGen.enableGate(gatePositions)
    //    printer.print()
    //    gatePositions = mazeGen.solvable()
    //}

    printer.print()

    do {
        val exploredPositions = mazeGen.expandable()

        val couldExpand = mazeGen.expand(exploredPositions)

        printer.print()
    } while (couldExpand)

    mazeGen.addPadding()

    printer.print()
}

class MazeGen(var rows: Int, var cols: Int) {

    val SIDE_TOP = 1
    val SIDE_RIGHT = 2
    val SIDE_BOTTOM = 3
    val SIDE_LEFT = 4

    val STATE_ALL_FREE : Byte = 0
    val STATE_ALL_BLOCKED : Byte = 2 * 2 * 2 * 2 - 1

    val MIN_DIST = 0 // TODO That constant is kinda obsolete, consider to remove it!

    // Array structure: [row][col]
    // 1. Bit: top
    // 2. Bit: right
    // 3. Bit: bottom
    // 4. Bit: left
    // Bit set = blocked, bit not set = free
    var maze : Array<ByteArray> = arrayOf(byteArrayOf(STATE_ALL_FREE))

    // Attention: The position of the start field cannot be the same as the exit field!
    var startRow : Int = rows - 1 // rows / 2
    var startCol : Int = 0 //cols / 2
    var exitRow : Int = 0
    var exitCol : Int = cols - 1
    var rowDirection = 0
    var colDirection = 0

    var counter : Long = 0

    val rand = java.util.Random()

    init {
        if (rows < 3 || cols < 3) {
            throw Exception("Error: Rows and cols cannot be less than 3!")
        }
        if (rows % 2 == 0 || cols % 2 == 0) {
            throw Exception("Error: Rows and cols cannot be even, they have to be odd!")
        }

        if (startRow > exitRow) rowDirection = SIDE_TOP
        if (startRow < exitRow) rowDirection = SIDE_BOTTOM
        if (startCol > exitCol) colDirection = SIDE_LEFT
        if (startCol < exitCol) colDirection = SIDE_RIGHT
    }

    /**
     * Generates a maze. Attention: This class offers more methods that increase the quality of
     * the generated maze so you should call them after this method!
     */
    fun generate() {
        //rand.setSeed(counter++) // Use this to produce predictable mazes

        maze = Array(rows, { ByteArray(cols, { STATE_ALL_FREE }) })

        /*
        if (rand.nextInt(2) == 0) {
            exitRow = rand.nextInt(2)
            exitCol = rand.nextInt(cols)
        } else {
            exitRow = rand.nextInt(rows)
            exitCol = rand.nextInt(2)
        }
        */

        for (row in 0..(rows - 1)) {
            for (col in 0..(cols - 1)) {
                // Set state of field to a random value
                //maze[row][col] = rand.nextInt(STATE_ALL_BLOCKED.toInt() + 1).toByte()
                maze[row][col] = (rand.nextInt(STATE_ALL_BLOCKED.toInt()) + 1).toByte()

                // Block a randomly chosen side
                val side = rand.nextInt(4) + 1
                //setSideBlocked(row, col, side)

                adjustNeighbourWalls(row, col)

                // Create a border around the whole maze
                if (row == 0) {
                    setSideBlocked(row, col, SIDE_TOP)
                }
                if (row == rows - 1) {
                    setSideBlocked(row, col, SIDE_BOTTOM)
                }
                if (col == 0) {
                    setSideBlocked(row, col, SIDE_LEFT)
                }
                if (col == cols - 1) {
                    setSideBlocked(row, col, SIDE_RIGHT)
                }

                // Open the border around the maze if this field is the exit field
                if (row == exitRow && col == exitCol) {
                    if (row == 0) {
                        setSideFree(row, col, SIDE_TOP)
                    }
                    if (row == rows - 1) {
                        setSideFree(row, col, SIDE_BOTTOM)
                    }
                    if (col == 0) {
                        setSideFree(row, col, SIDE_LEFT)
                    }
                    if (col == cols - 1) {
                        setSideFree(row, col, SIDE_RIGHT)
                    }
                }
            }
        }

        // Ensure there are no completely blocked fields
        for (row in 0..(rows - 1)) {
            for (col in 0..(cols - 1)) {
                if (maze[row][col] == STATE_ALL_BLOCKED) {
                    while (true) {
                        val side = rand.nextInt(4) + 1

                        if (
                            (row > 0 || side != SIDE_TOP) &&
                            (row < rows - 1 || side != SIDE_BOTTOM) &&
                            (col > 0 || side != SIDE_LEFT) &&
                            (col > cols - 1 || side != SIDE_RIGHT))
                        {
                            setSideFree(row, col, side)
                            adjustNeighbourWalls(row, col, true)
                            break
                        }
                    }
                }
            }
        }
    }

    /**
     * Tries to solve a maze. Returns an empty array list if it is solvable.
     * Otherwise it returns a non-empty array list with positions of fields that can
     * be changed to enhance the reachable part of the maze.
     * It does not matter if the start position is completely blocked.
     */
    fun solvable() : ArrayList<IntArray> {
        var unexploredPositions = arrayListOf<IntArray>(intArrayOf(startRow, startCol))
        var exploredPositions = arrayListOf<IntArray>()
        var gatePositions = arrayListOf<IntArray>()

        while (unexploredPositions.size > 0) {
            for (pos in unexploredPositions) {
                val row = pos[0]
                val col = pos[1]

                if (row == exitRow && col == exitCol) {
                    return arrayListOf<IntArray>()
                }

                // Top
                if (! isSideBlocked(row, col, SIDE_TOP)) {
                    val unexplored = unexploredPositions.find { it[0] == row - 1 && it[1] == col }
                    if (unexplored === null) {
                        val explored = exploredPositions.find { it[0] == row - 1 && it[1] == col }
                        if (explored === null) {
                            unexploredPositions.add(intArrayOf(row - 1, col))
                            break
                        }
                    }
                }
                // Right
                if (! isSideBlocked(row, col, SIDE_RIGHT)) {
                    val unexplored = unexploredPositions.find { it[0] == row && it[1] == col + 1}
                    if (unexplored === null) {
                        val explored = exploredPositions.find { it[0] == row && it[1] == col + 1 }
                        if (explored === null) {
                            unexploredPositions.add(intArrayOf(row, col + 1))
                            break
                        }
                    }
                }
                // Bottom
                if (! isSideBlocked(row, col, SIDE_BOTTOM)) {
                    val unexplored = unexploredPositions.find { it[0] == row + 1 && it[1] == col }
                    if (unexplored === null) {
                        val explored = exploredPositions.find { it[0] == row + 1 && it[1] == col }
                        if (explored === null) {
                            unexploredPositions.add(intArrayOf(row + 1, col))
                            break
                        }
                    }
                }
                // Left
                if (! isSideBlocked(row, col, SIDE_LEFT)) {
                    val unexplored = unexploredPositions.find { it[0] == row && it[1] == col - 1 }
                    if (unexplored === null) {
                        val explored = exploredPositions.find { it[0] == row && it[1] == col - 1}
                        if (explored === null) {
                            unexploredPositions.add(intArrayOf(row, col - 1))
                            break
                        }
                    }
                }

                // All sides are blocked or known -> field is explored now
                exploredPositions.add(pos)
                unexploredPositions.remove(pos)

                if (
                    (row > 0 && rowDirection == SIDE_TOP && isSideBlocked(row, col, SIDE_TOP)) ||
                    (col < cols - 1 && colDirection == SIDE_RIGHT && isSideBlocked(row, col, SIDE_RIGHT)) ||
                    (row < rows - 1 && rowDirection == SIDE_BOTTOM && isSideBlocked(row, col, SIDE_BOTTOM)) ||
                    (col > 0 && colDirection == SIDE_LEFT && isSideBlocked(row, col, SIDE_LEFT)))
                {
                    gatePositions.add(pos)
                }

                break
            }
        }

        return gatePositions
    }

    /**
     * Expects an array list of field positions that limit the reachable area.
     * Chooses the last position in the array and removes it walls so the reachable area grows.
     */
    fun enableGate(gatePositions : ArrayList<IntArray>) {
        val gatePosition =  gatePositions.last()
        val row = gatePosition[0]
        val col = gatePosition[1]

        var found = false
        if (row > 0 && rowDirection == SIDE_TOP && isSideBlocked(row, col, SIDE_TOP)) {
            setSideFree(row, col, SIDE_TOP)
            found = true
        }
        if (col < cols - 1 && colDirection == SIDE_RIGHT && isSideBlocked(row, col, SIDE_RIGHT)) {
            setSideFree(row, col, SIDE_RIGHT)
            found = true
        }
        if (row < rows - 1 && rowDirection == SIDE_BOTTOM && isSideBlocked(row, col, SIDE_BOTTOM)) {
            setSideFree(row, col, SIDE_BOTTOM)
            found = true
        }
        if (col > 0 && colDirection == SIDE_LEFT && isSideBlocked(row, col, SIDE_LEFT)) {
            setSideFree(row, col, SIDE_LEFT)
            found = true
        }

        if (! found) {
            throw Exception("Error: Could not open gate at $row/$col")
        }

        adjustNeighbourWalls(row, col, true)
    }

    /**
     * Returns an array list of positions of fields that are reachable from the start position.
     * Pretty much copy-and-paste-code from the solvable() method
     */
    fun expandable() : ArrayList<IntArray> {
        var unexploredPositions = arrayListOf<IntArray>(intArrayOf(startRow, startCol))
        var exploredPositions = arrayListOf<IntArray>()

        while (unexploredPositions.size > 0) {
            for (pos in unexploredPositions) {
                val row = pos[0]
                val col = pos[1]

                // Top
                if (! isSideBlocked(row, col, SIDE_TOP)) {
                    val unexplored = unexploredPositions.find { it[0] == row - 1 && it[1] == col }
                    if (unexplored === null) {
                        val explored = exploredPositions.find { it[0] == row - 1 && it[1] == col }
                        if (explored === null) {
                            if (distance(row - 1, col, exitRow, exitCol) > MIN_DIST) {
                                unexploredPositions.add(intArrayOf(row - 1, col))
                                break
                            }
                        }
                    }
                }
                // Right
                if (! isSideBlocked(row, col, SIDE_RIGHT)) {
                    val unexplored = unexploredPositions.find { it[0] == row && it[1] == col + 1 }
                    if (unexplored === null) {
                        val explored = exploredPositions.find { it[0] == row && it[1] == col + 1 }
                        if (explored === null) {
                            if (distance(row, col + 1, exitRow, exitCol) > MIN_DIST) {
                                unexploredPositions.add(intArrayOf(row, col + 1))
                                break
                            }
                        }
                    }
                }
                // Bottom
                if (! isSideBlocked(row, col, SIDE_BOTTOM)) {
                    val unexplored = unexploredPositions.find { it[0] == row + 1 && it[1] == col }
                    if (unexplored === null) {
                        val explored = exploredPositions.find { it[0] == row + 1 && it[1] == col }
                        if (explored === null) {
                            if (distance(row + 1, col, exitRow, exitCol) > MIN_DIST) {
                                unexploredPositions.add(intArrayOf(row + 1, col))
                                break
                            }
                        }
                    }
                }
                // Left
                if (! isSideBlocked(row, col, SIDE_LEFT)) {
                    val unexplored = unexploredPositions.find { it[0] == row && it[1] == col - 1 }
                    if (unexplored === null) {
                        val explored = exploredPositions.find { it[0] == row && it[1] == col - 1 }
                        if (explored === null) {
                            if (distance(row , col - 1, exitRow, exitCol) > MIN_DIST) {
                                unexploredPositions.add(intArrayOf(row, col - 1))
                                break
                            }
                        }
                    }
                }

                // All sides are blocked or known or restricted -> field is explored now
                exploredPositions.add(pos)
                unexploredPositions.remove(pos)

                break
            }
        }

        return exploredPositions
    }

    /**
     * Changes the walls of one field in the exploredPositions array so that
     * the reachable area expands. Returns true if expansion was possible, otherwise false.
     */
    fun expand(exploredPositions: ArrayList<IntArray>) : Boolean {
        for (explored in exploredPositions) {
            val row = explored[0]
            val col = explored[1]

            if (row > 0 && isSideBlocked(row, col, SIDE_TOP)) {
                val alsoExplored = exploredPositions.find { it[0] == row - 1 && it[1] == col }
                if (alsoExplored === null && distance(row - 1, col, exitRow, exitCol) > MIN_DIST) {
                    setSideFree(row, col, SIDE_TOP)
                    adjustNeighbourWalls(row, col, true)
                    return true
                }
            }
            if (col < cols - 1 && isSideBlocked(row, col, SIDE_RIGHT)) {
                val alsoExplored = exploredPositions.find { it[0] == row && it[1] == col + 1 }
                if (alsoExplored === null && distance(row, col + 1, exitRow, exitCol) > MIN_DIST) {
                    setSideFree(row, col, SIDE_RIGHT)
                    adjustNeighbourWalls(row, col, true)
                    return true
                }
            }
            if (row < rows - 1 && isSideBlocked(row, col, SIDE_BOTTOM)) {
                val alsoExplored = exploredPositions.find { it[0] == row + 1 && it[1] == col }
                if (alsoExplored === null && distance(row + 1 , col, exitRow, exitCol) > MIN_DIST) {
                    setSideFree(row, col, SIDE_BOTTOM)
                    adjustNeighbourWalls(row, col, true)
                    return true
                }
            }
            if (col > 0 && isSideBlocked(row, col, SIDE_LEFT)) {
                val alsoExplored = exploredPositions.find { it[0] == row && it[1] == col - 1 }
                if (alsoExplored === null && distance(row, col - 1, exitRow, exitCol) > MIN_DIST) {
                    setSideFree(row, col, SIDE_LEFT)
                    adjustNeighbourWalls(row, col, true)
                    return true
                }
            }
        }

        return false
    }

    /**
     * Finds areas of four adjacent fields that have no walls between them and adds a wall
     */
    fun addPadding() {
        for (row in 0..(rows - 2)) {
            for (col in 0..(cols - 2)) {
                // Find an area of four adjacent fields that have no walls between them
                val isEmptyArea =
                    isSideFree(row, col, SIDE_BOTTOM) && isSideFree(row, col, SIDE_RIGHT) && // left top
                    isSideFree(row, col + 1, SIDE_LEFT) && isSideFree(row, col + 1, SIDE_BOTTOM) && // right top
                    isSideFree(row + 1, col + 1, SIDE_TOP) && isSideFree(row + 1, col + 1, SIDE_LEFT) && // right bottom
                    isSideFree(row + 1, col, SIDE_RIGHT) && isSideFree(row + 1, col, SIDE_TOP) // left bottom

                if (isEmptyArea) {
                    val side = rand.nextInt(4) + 1 // The point of view is the center of the 4 fields

                    when (side) {
                        SIDE_TOP -> {
                            setSideBlocked(row, col, SIDE_RIGHT)
                            adjustNeighbourWalls(row, col, true)
                        }
                        SIDE_RIGHT -> {
                            setSideBlocked(row, col + 1, SIDE_BOTTOM)
                            adjustNeighbourWalls(row, col + 1, true)
                        }
                        SIDE_BOTTOM -> {
                            setSideBlocked(row + 1, col + 1, SIDE_LEFT)
                            adjustNeighbourWalls(row + 1, col + 1, true)
                        }
                        SIDE_LEFT -> {
                            setSideBlocked(row + 1, col, SIDE_TOP)
                            adjustNeighbourWalls(row + 1, col, true)
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the direct distance between two positions
     */
    private fun distance(row1 : Int, col1 : Int, row2 : Int, col2 : Int) : Int {
        val rowDist = row1 - row2
        val colDist = col1 - col2
        return Math.sqrt( (rowDist * rowDist + colDist * colDist).toDouble() ).toInt()
    }

    /**
     * Sets a given side to blocked (1)
     */
    private fun setSideBlocked(row : Int, col : Int, side : Int) {
        val mask : Byte = Math.pow(2.0, (side - 1).toDouble()).toByte()
        maze[row][col] = maze[row][col] or mask
    }

    /**
     * Returns true if a given side is blocked (1), false if it is free (0)
     */
    fun isSideBlocked(row : Int, col : Int, side : Int) : Boolean {
        val mask : Byte = Math.pow(2.0, (side - 1).toDouble()).toByte()
        val masked : Byte = maze[row][col] and mask
        return masked > 0
    }

    /**
     * Sets a given side to free (0)
     */
    private fun setSideFree(row : Int, col : Int, side : Int) {
        if (side != SIDE_TOP && side != SIDE_RIGHT && side != SIDE_BOTTOM && side != SIDE_LEFT) {
            throw Exception("Error: Invalid side value: " + side)
        }

        val mask : Byte = Math.pow(2.0, (side - 1).toDouble()).toByte() xor 15
        maze[row][col] = maze[row][col] and mask
    }

    /**
     * Returns true if a given side is free (0), false if it is blocked (1)
     */
    fun isSideFree(row : Int, col : Int, side : Int) : Boolean {
        return ! isSideBlocked(row, col, side)
    }

    /**
     * "Walls" are actually double-walls so we have to ensure that
     * a wall between two fields is a wall for both of them.
     * This method ensures this for the fields above and left of
     * a given field - but not for the fields right and below to it!
     */
    fun adjustNeighbourWalls(row : Int, col : Int, adjustAll : Boolean = false) {
        // Adjust state of field above
        if (row > 0) {
            if (isSideBlocked(row, col, SIDE_TOP)) {
                setSideBlocked(row - 1, col, SIDE_BOTTOM)
            } else {
                setSideFree(row - 1, col, SIDE_BOTTOM)
            }
        }

        // Adjust state of left field
        if (col > 0) {
            if (isSideBlocked(row, col, SIDE_LEFT)) {
                setSideBlocked(row, col - 1, SIDE_RIGHT)
            } else {
                setSideFree(row, col - 1, SIDE_RIGHT)
            }
        }

        if (adjustAll) {
            // Adjust state of field below
            if (row < rows - 1) {
                if (isSideBlocked(row, col, SIDE_BOTTOM)) {
                    setSideBlocked(row + 1, col, SIDE_TOP)
                } else {
                    setSideFree(row + 1, col, SIDE_TOP)
                }
            }

            // Adjust state of right field
            if (col < cols - 1) {
                if (isSideBlocked(row, col, SIDE_RIGHT)) {
                    setSideBlocked(row, col + 1, SIDE_LEFT)
                } else {
                    setSideFree(row, col + 1, SIDE_LEFT)
                }
            }
        }
    }

}

open class MazePrinter(mazeGen : MazeGen) {

    val mazeGen = mazeGen

    open fun print() {
        throw Exception("Error: The inheriting class has to override this method!")
    }

    /**
     * Shortcut for mazeGen.isSideBlocked()
     */
    fun isSideBlocked(row : Int, col : Int, side : Int) : Boolean {
        return mazeGen.isSideBlocked(row, col, side)
    }

}

class ConsolePrinter(mazeGen : MazeGen) : MazePrinter(mazeGen) {

    var rows : Int = 0
    var cols : Int = 0

    init {
        rows = mazeGen.rows
        cols = mazeGen.cols
    }

    /**
     * Prints the whole maze to the console (ASCII art style) and to a text file.
     */
    override fun print() {
        var output : String = ""

        for (row in 0..(rows - 1)) {
            output += printRow(row)
        }

        print(output)

        File("maze.txt").writeText(output)
    }

    /**
     * Prints a row of fields of the maze.
     */
    fun printRow(row : Int) : String
    {
        var output : String = ""

        for (col in 0..(cols - 1)) {
            output += printFieldTop(row, col)
        }
        output += "\n"
        print(output)

        for (col in 0..(cols - 1)) {
            output += printFieldMid(row, col)
        }
        output += "\n"
        print(output)

        for (col in 0..(cols - 1)) {
            output += printFieldBottom(row, col)
        }
        output += "\n"
        print(output)

        return output
    }

    fun printFieldTop(row: Int, col : Int) : String  {
        var output : String = ""

        if (isSideBlocked(row, col, mazeGen.SIDE_TOP) || isSideBlocked(row, col, mazeGen.SIDE_LEFT) ) {
            output += ("█")
        } else {
            output += (" ")
        }
        if (isSideBlocked(row, col, mazeGen.SIDE_TOP)) {
            output += ("█")
        } else {
            output += (" ")
        }
        if (isSideBlocked(row, col, mazeGen.SIDE_TOP) || isSideBlocked(row, col, mazeGen.SIDE_RIGHT) ) {
            output += ("█")
        } else {
            output += (" ")
        }

        print(output)

        return output
    }

    fun printFieldMid(row: Int, col : Int) : String {
        var output : String = ""

        if (isSideBlocked(row, col, mazeGen.SIDE_LEFT) ) {
            output += ("█")
        } else {
            output += (" ")
        }
        if (isSideBlocked(row, col, mazeGen.SIDE_TOP) && isSideBlocked(row, col, mazeGen.SIDE_RIGHT)
                && isSideBlocked(row, col, mazeGen.SIDE_LEFT) && isSideBlocked(row, col, mazeGen.SIDE_BOTTOM)) {
            output += ("█")
        } else {
            output += (" ")
        }
        if (isSideBlocked(row, col, mazeGen.SIDE_RIGHT)) {
            output += ("█")
        } else {
            output += (" ")
        }

        print(output)

        return output
    }

    fun printFieldBottom(row: Int, col : Int) : String {
        var output : String = ""

        if (isSideBlocked(row, col, mazeGen.SIDE_BOTTOM) || isSideBlocked(row, col, mazeGen.SIDE_LEFT) ) {
            output += ("█")
        } else {
            output += (" ")
        }
        if (isSideBlocked(row, col, mazeGen.SIDE_BOTTOM)) {
            output += ("█")
        } else {
            output += (" ")
        }
        if (isSideBlocked(row, col, mazeGen.SIDE_BOTTOM) || isSideBlocked(row, col,mazeGen. SIDE_RIGHT) ) {
            output += ("█")
        } else {
            output += (" ")
        }

        print(output)

        return output
    }
}

class HtmlPrinter(mazeGen : MazeGen) : MazePrinter(mazeGen) {

    var rows : Int = 0
    var cols : Int = 0

    init {
        rows = mazeGen.rows
        cols = mazeGen.cols
    }

    /**
     * Prints the whole maze to the console (as HTML code) and to an HTML file.
     */
    override fun print() {
        val fieldSize = 20 // px

        var output : String = ""

        output += ("\n<style>")
        output += (".maze { background-color: white; margin-bottom: 20px }")
        output += (".maze .maze-row { display: table-row }")
        output += (".maze .maze-field { width: " + fieldSize + "px; height: " + fieldSize + "px; float: left; border: 1px solid transparent }")
        output += (".maze .maze-field.top { border-top: 1px solid black }")
        output += (".maze .maze-field.right { border-right: 1px solid black }")
        output += (".maze .maze-field.bottom { border-bottom: 1px solid black }")
        output += (".maze .maze-field.left { border-left: 1px solid black }")
        //output += (".maze .maze-field.top.right.left.bottom { background-color: black }")
        output += (".maze .maze-field.start { background-color: silver }")
        output += (".maze .maze-field.exit { background-color: silver }")
        output += (".maze .maze-field.marked { background-color : #98FB98 }")
        output += (".maze-field:hover::before { content: attr(data-title) }")
        output += ("</style>\n")

        output += ("<div class=\"maze\">")
        for (row in 0..(rows - 1)) {
            output += printRow(row)
        }
        output += ("</div>")

        output += ("<script>var elements = document.querySelectorAll('.maze-field');" +
                "for (var i = 0; i < elements.length; i++) {" +
                "  var element = elements[i];" +
                "  element.addEventListener('click', function()" +
                "  {" +
                "    this.classList.toggle('marked');" +
                "  });" +
                " }</script>\n")

        print(output)

        File("maze.html").writeText("<!DOCTYPE html><html><head><title>Maze</title></head><body>$output</body></html>")
    }

    /**
     * Prints a row of fields of the maze.
     */
    fun printRow(row : Int) : String
    {
        var output : String = "<div class=\"maze-row\">"
        for (col in 0..(cols - 1)) {
            output += printField(row, col)
        }
        output += ("</div>")

        print(output)

        return output
    }

    /**
     * Prints a single field
     */
    fun printField(row : Int, col : Int) : String {
        var output : String = ("<div class=\"maze-field ")

        if (isSideBlocked(row, col, mazeGen.SIDE_TOP)) {
            output += ("top ")
        }
        if (isSideBlocked(row, col, mazeGen.SIDE_RIGHT)) {
            output += ("right ")
        }
        if (isSideBlocked(row, col, mazeGen.SIDE_BOTTOM)) {
            output += ("bottom ")
        }
        if (isSideBlocked(row, col, mazeGen.SIDE_LEFT)) {
            output += ("left ")
        }
        if (row == mazeGen.startRow && col == mazeGen.startCol) {
            output += ("start ")
        }
        if (row == mazeGen.exitRow && col == mazeGen.exitCol) {
            output += ("exit ")
        }

        output += ("\" data-title=\"$row/$col\"></div>")

        print(output)

        return output
    }
}
