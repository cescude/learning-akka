object Repl {
  def repl(prompt : String)(implicit repl_block : PartialFunction[(String, Seq[String]),Unit] = {case _ =>}) : Unit =
    scala.io.Source.stdin.getLines() foreach { line : String =>
      print(prompt)

      val args = line split " +"

      if (args.size > 0) {
        repl_block.lift((args.head, args.tail))
      }
    }
}
