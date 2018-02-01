package samples

/*
* Example from Group Dynamics interview, 1/31/2018
* Interviewer: Vladimir Metveev
* */

object Main {
  def main(args: Array[String]): Unit = {
    val words = Vector("a", "bc", "de", "abc", "bca", "cab")
    val collection = AnagramCollection(words)
    println(collection.getAnagrams("bca")) // Vector("abc", "bca", "cba")
  }
}

class AnagramCollection(words: Iterable[String]) {
  val wds = words
  def getUniqueCheckSum(str: String) : Int = {
    val res = str.foldLeft(0)((acc, elem) => acc + elem.toInt)
    println(s"String: $str, Res: $res")
    res
  }

  def getAnagrams(word: String) = for {
      wd <- words
      if wd.length == word.length
      if getUniqueCheckSum(wd) == getUniqueCheckSum(word)
    } yield wd
}

object AnagramCollection {
  def apply(words: Iterable[String]): AnagramCollection = {
    new AnagramCollection(words)
  }
}
