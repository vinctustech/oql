//package com.vinctus.oql2
//
//import com.vinctus.oql2.OQLParser.{GroupContext, LabelContext, LogicalExpressionContext, OrderContext, WhenContext}
//import org.antlr.v4.runtime.{CharStreams, CommonTokenStream, ConsoleErrorListener}
//
//import scala.collection.mutable
//import scala.jdk.CollectionConverters._
//
//object OQLParse {
//
//  def instantiate(input: String): OQLParser = {
//    val charStream = CharStreams.fromString(input)
//    val lexer = new OQLLexer(charStream)
//    val tokens = new CommonTokenStream(lexer)
//    val parser = new OQLParser(tokens)
//    val errors = new ErrorListener(input)
//
//    lexer.removeErrorListener(ConsoleErrorListener.INSTANCE)
//    parser.removeErrorListener(ConsoleErrorListener.INSTANCE)
//    lexer.addErrorListener(errors)
//    parser.addErrorListener(errors)
//    parsingError = false
//    parser
//  }
//
//  def apply(input: String): Option[OQLAST] = {
//    val res = instantiate(input).command
//
//    if (parsingError) None
//    else Some(res.c)
//  }
//
//  def logicalExpression(input: String): Option[OQLExpression] = {
//    val res = instantiate(input).logicalExpression
//
//    if (parsingError) None
//    else Some(res.e)
//  }
//
//  def label(ctx: LabelContext, proj: Any): Ident =
//    ctx match {
//      case null =>
//        proj match {
//          case id: Ident                                  => id
//          case OQLQuery(resource, _, _, _, _, _, _, _, _) => resource
//          case AttributeOQLExpression(List(id), _)        => id
//          case ReferenceOQLExpression(List(id))           => id
//          case ParameterOQLExpression(id)                 => id
//        }
//      case _ => ctx.id
//    }
//
//  def label(ctx: LabelContext, f: Ident, a: Any): Ident =
//    ctx match {
//      case null =>
//        a match {
//          case AttributeOQLExpression(List(id), _) => Ident(s"${f.s}_${id.s}", f.pos)
//          case StarOQLExpression                   => f
//        }
//      case _ => ctx.id
//    }
//
//  def project(ps: mutable.Buffer[OQLProject]): List[OQLProject] = if (ps eq null) Nil else ps.toList
//
//  def select(ctx: LogicalExpressionContext): Option[OQLExpression] = if (ctx eq null) None else Some(ctx.e)
//
//  def group(ctx: GroupContext): Option[List[OQLExpression]] = if (ctx eq null) None else Some(ctx.es.toList)
//
//  def order(ctx: OrderContext): Option[List[OQLOrdering]] = if (ctx eq null) None else Some(ctx.os.toList)
//
//  def ordering(dir: String, nulls: String): String =
//    (dir, nulls) match {
//      case (null, null) | ("ASC" | "asc", null) => "ASC NULLS FIRST"
//      case (null, nulls) if nulls ne null       => s"ASC NULLS ${nulls.toUpperCase}"
//      case (_, null)                            => "DESC NULLS LAST"
//      case (dir, nulls)                         => s"${dir.toUpperCase} NULLS ${nulls.toUpperCase}"
//    }
//
//  def restrict(limit: String, offset: String): OQLRestrict =
//    OQLRestrict(Option(limit) map (_.toInt), Option(offset) map (_.toInt))
//
//  def caseExpression(whens: java.util.List[WhenContext], els: ExpressionContext): OQLExpression =
//    CaseOQLExpression(whens.asScala.toList map (_.w), Option(els) map (_.e))
//
//  val star: List[OQLExpression] = List(StarOQLExpression)
//
//}
