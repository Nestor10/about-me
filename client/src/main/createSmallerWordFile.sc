import java.io.{File, FileWriter}
import scala.io.{BufferedSource, Source}


val file = new File("/home/fry/git_repos/hello_world_laminar/src/main/resources/words_alpha.txt")
val newFile = new File("/home/fry/git_repos/hello_world_laminar/src/main/resources/new_words_alpha.txt")




def readFile[T](f: File)(handler: BufferedSource => T): T = {
  val resource = Source.fromFile(f)
  try {
    handler(resource)
  } finally {
    resource.close()
  }
}

def writeFile[T](f: File)(handler: FileWriter => T): T = {
  val resource = new FileWriter(f)
  try {
    handler(resource)
  } finally {
    resource.close()
  }
}

readFile(file){
  buff =>
    writeFile(newFile){
      writer =>
        buff
          .getLines()
          .filter(u => u.length > 2 && u.length <= 5)
          .map(_ + "\n")
          .foreach(writer.write)
    }
}

println("done")