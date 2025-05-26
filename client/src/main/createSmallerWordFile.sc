import java.io.{File, FileWriter}
import scala.io.{BufferedSource, Source}
import scala.util.Try // Good for handling potential errors with URL fetching

val wordsUrl = "https://raw.githubusercontent.com/MichaelWehar/Public-Domain-Word-Lists/master/5000-more-common.txt"
val newFile = new File("/home/fry/git_repos/hello_world_laminar/client/src/main/resources/words_alpha.txt")

def writeFile[T](f: File)(handler: FileWriter => T): T = {
  val resource = new FileWriter(f)
  try {
    handler(resource)
  } finally {
    resource.close()
  }
}

def readSource[T](createSource: () => BufferedSource)(handler: BufferedSource => T): Try[T] = {
  Try {
    val resource = createSource()
    try {
      handler(resource)
    } finally {
      resource.close()
    }
  }
}

println(s"Fetching words from: $wordsUrl")

readSource(() => Source.fromURL(wordsUrl)) { buff =>
  writeFile(newFile) { writer =>
    println(s"Filtering words and writing to: ${newFile.getAbsolutePath}")
    val linesProcessed = buff
      .getLines()
      .filter(word => word.length > 2 && word.length <= 8)
      .map(_ + "\n")
      .foldLeft(0) { (count, line) =>
        writer.write(line)
        count + 1
      }
    println(s"Processed and wrote $linesProcessed words.")
  }
} match {
  case scala.util.Success(_) => println("Successfully created smaller word file.")
  case scala.util.Failure(e) =>
    System.err.println(s"An error occurred: ${e.getMessage}")
    e.printStackTrace()
}

println("done")