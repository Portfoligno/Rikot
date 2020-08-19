@file:Suppress("ObjectPropertyName", "NonAsciiCharacters")
package rikot.compiler

import rikot.compiler.State.*
import rikot.compiler.utility.*
import rikot.utility.Seq
import rikot.utility.plus
import rikot.utility.seqOf

private
sealed class State {
  data class Literal(val offset: Int) : State()
  data class NewLine(val literal: Literal) : State()
  data class Hyphen(val offset: Int, val literal: Literal) : State()
  object ClosingCarriageReturn : State()

  data class Brace(val offset: Int, val literal: Literal) : State()
  data class DoubleBrace(val level: Nat) : State()
  object DoubleBraceWithSpaces : State() // and/or with new lines

  // `inlining` is null if the construct is hyphen based
  data class VariableName(val offset: Int, val inlining: Unit?) : State()
  data class VariableNameWithSpace(val offset: Int, val variableName: VariableName) : State()
  data class VariableNameWithNewLine(val name: String, val inlining: Unit?) : State()

  data class Colon(val name: String, val inlining: Unit?) : State()

  data class VariableType(val offset: Int, val colon: Colon) : State()
  data class VariableTypeWithSpace(val offset: Int, val variableType: VariableType) : State()

  data class DoubleHyphen(val hyphen: Hyphen) : State()
  object DoubleHyphenWithBrace : State()

  data class InlineLiteral(val offset: Int, val level: Nat) : State()
  data class InlineLiteralWithBracket(val offset: Int, val level: Nat, val inlineLiteral: InlineLiteral) : State()

  object InlineComment : State()
  object InlineCommentWithNumberSign : State()
  object FullLineComment : State()
  object FullLineCommentWithNumberSign : State()
  object FullLineCommentWithNumberSignWithHyphen : State()

  data class VariableTypeWithNewLine(val inlining: Unit?) : State()
  data class ClosingInnerBrace(val inlining: Unit?) : State()
  object ClosingInnerHyphen : State()
  object ClosingOuterHyphen : State()
}


private val ` ` by CodePoint
private val `⧹r` = CodePoint('\r')
private val `⧹n` = CodePoint('\n')
private val `-` by CodePoint
private val `{` by CodePoint
private val `}` by CodePoint
private val `⁚` by CodePoint.replacing('⁚' to ':')
private val `⁅` by CodePoint.replacing('⁅' to '[')
private val `⁆` by CodePoint.replacing('⁆' to ']')
private val `#` by CodePoint

private val `'` by CodePoint
private val `&` by CodePoint
private val `,` by CodePoint

private
fun CodePoint.isValidIdentifier() =
    when (this) {
      `-`, `'`, `&`, `,` -> true
      else -> isLetterOrDigit()
    }

private
fun VariableType.toNode(type: String) =
    when (colon.inlining) {
      null -> Node.PlaceholderVariable(colon.name, type)
      else -> ExpressionNode.Variable(colon.name, type)
    }

private
fun String.terminalNodes(literal: Literal): Seq<Node> =
    seqOf(ExpressionNode.Literal(when (val i = literal.offset) {
      length -> ""
      else -> when {
        endsWith("\r\n") -> substring(i, length - 2)
        else -> when (lastOrNull()) {
          '\r', '\n' -> substring(i, length - 1)
          else -> substring(i)
        }
      }
    }))

private
fun failing(message: String): IllegalArgumentException =
    IllegalArgumentException(message)

private
fun Int.failing(subject: String, message: String): IllegalArgumentException =
    IllegalArgumentException("$subject at position $this $message")

private
fun Int.failing(message: String): IllegalArgumentException =
    IllegalArgumentException("$message at position $this")


object RikotParser {
  operator fun invoke(template: String): Seq<Node> =
      template
          .parse(
              Seq.Cons(IndexedValue(0, `⧹n`)) {
                template.codePointsWithEndIndexes
              },
              NewLine(Literal(0))
          )
          .filterNot {
            (it as? ExpressionNode.Literal)?.value == ""
          }
          .map {
            // Temporary workaround
            it.escapeSpaces()
          }
}

// Mutes the 'Recursive call is not a tail call' warning
private
fun String.parseNext(lastSeq: Seq.Cons<IndexedValue<CodePoint>>, state: State): Seq<Node> =
    parse(lastSeq, state)

private
tailrec fun String.parse(
    lastSeq: Seq.Cons<IndexedValue<CodePoint>>,
    state: State
): Seq<Node> {
  val offset = lastSeq.head.index
  val seq = lastSeq.tail() as? Seq.Cons
  val c = seq?.head?.value

  return when (state) {
    // Literal parsing
    is Literal -> when (c) {
      null -> terminalNodes(state)
      `⧹r`, `⧹n` -> parse(seq, NewLine(state))
      `{` -> parse(seq, Brace(offset, state))
      else -> parse(seq, state)
    }
    is NewLine -> when (c) {
      null -> terminalNodes(state.literal)
      `⧹r`, `⧹n` -> parse(seq, NewLine(state.literal))
      `{` -> parse(seq, Brace(offset, state.literal))
      `-` -> parse(seq, Hyphen(offset, state.literal))
      else -> parse(seq, state.literal)
    }
    is Hyphen -> when (c) {
      null -> terminalNodes(state.literal)
      `⧹r`, `⧹n` -> parse(seq, NewLine(state.literal))
      `{` -> parse(seq, Brace(offset, state.literal))
      `-` -> parse(seq, DoubleHyphen(state))
      else -> parse(seq, state.literal)
    }
    ClosingCarriageReturn -> when (c) {
      null -> seqOf()
      `⧹r` -> parse(seq, NewLine(Literal(offset)))
      `⧹n` -> parse(seq, NewLine(Literal(seq.head.index)))
      `{` -> parse(seq, Brace(offset, Literal(offset)))
      else -> parse(seq, Literal(offset))
    }

    // Variable parsing
    is Brace -> when (c) {
      null -> terminalNodes(state.literal)
      ` ` -> parse(seq, state.literal)
      `⧹r`, `⧹n` -> parse(seq, NewLine(state.literal))
      `{` -> ExpressionNode.Literal(substring(state.literal.offset, state.offset)) + {
        parseNext(seq, DoubleBrace(Nat.Zero))
      }
      `⁅` -> ExpressionNode.Literal(substring(state.literal.offset, state.offset)) + {
        parseNext(seq, InlineLiteral(seq.head.index, Nat.Zero))
      }
      `#` -> ExpressionNode.Literal(substring(state.literal.offset, state.offset)) + {
        parseNext(seq, InlineComment)
      }
      else -> when {
        // Reserve symbols
        !c.isLetterOrDigit() -> throw offset.failing("Unexpected character '$c'", "following '$`{`'")
        else -> parse(seq, state.literal)
      }
    }
    is DoubleBrace -> when (c) {
      null -> throw failing("Expected a variable name, but the input has no more characters")
      ` `, `⧹r`, `⧹n` -> when (state.level) {
        Nat.Zero -> parse(seq, DoubleBraceWithSpaces)
        // Reserve triple braces
        else -> throw offset.failing("Unexpected character '$c'", "following '$`{`$`{`$`{`'")
      }
      `{` -> parse(seq, DoubleBrace(state.level.next))
      `⁅` -> parse(seq, InlineLiteral(seq.head.index, state.level.next))
      else -> when {
        c.isValidIdentifier() -> when (state.level) {
          Nat.Zero -> parse(seq, VariableName(offset, Unit))
          // Reserve triple braces
          else -> throw offset.failing("Unexpected character '$c'", "following '$`{`$`{`$`{`'")
        }
        else -> throw offset.failing("'$c'", "is not a valid identifier for a variable name")
      }
    }
    DoubleBraceWithSpaces -> when (c) {
      null -> throw failing("Expected a variable name, but the input has no more characters")
      ` `, `⧹r`, `⧹n` -> parse(seq, DoubleBraceWithSpaces)
      else -> when {
        c.isValidIdentifier() -> parse(seq, VariableName(offset, Unit))
        else -> throw offset.failing("'$c'", "is not a valid identifier for a variable name")
      }
    }
    is VariableName -> when (c) {
      null -> throw failing("Expected '$`⁚`', but the input has no more characters")
      ` ` -> parse(seq, VariableNameWithSpace(offset, state))
      `⧹r`, `⧹n` -> parse(seq, VariableNameWithNewLine(substring(state.offset, offset), state.inlining))
      `⁚` -> parse(seq, Colon(substring(state.offset, offset), state.inlining))
      else -> when {
        c.isValidIdentifier() -> parse(seq, state)
        else -> throw offset.failing("'$c'", "is not a valid identifier for a variable name")
      }
    }
    is VariableNameWithSpace -> when (c) {
      null -> throw failing("Expected '$`⁚`', but the input has no more characters")
      ` ` -> parse(seq, state)
      `⧹r`, `⧹n` -> parse(
          seq,
          VariableNameWithNewLine(substring(state.variableName.offset, state.offset), state.variableName.inlining)
      )
      `⁚` -> parse(seq, Colon(substring(state.variableName.offset, state.offset), state.variableName.inlining))
      else -> when {
        c.isValidIdentifier() -> parse(seq, state.variableName)
        else -> throw offset.failing("'$c'", "is not a valid identifier for a variable name")
      }
    }
    is VariableNameWithNewLine -> when (c) {
      null -> throw failing("Expected '$`⁚`', but the input has no more characters")
      ` `, `⧹r`, `⧹n` -> parse(seq, state)
      `⁚` -> parse(seq, Colon(state.name, state.inlining))
      else -> throw offset.failing("Expected '$`⁚`', but '$c' was found")
    }
    is Colon -> when (c) {
      null -> throw failing("Expected a variable type, but the input has no more characters")
      ` `, `⧹r`, `⧹n` -> parse(seq, state)
      else -> when {
        c.isValidIdentifier() -> parse(seq, VariableType(offset, state))
        else -> throw offset.failing("'$c'", "is not a valid identifier for a variable type")
      }
    }
    is VariableType -> when (c) {
      null -> throw failing("Expected '$`}`$`}`', but the input has no more characters")
      ` ` -> parse(seq, VariableTypeWithSpace(offset, state))
      `⧹r`, `⧹n` -> state.toNode(substring(state.offset, offset)) + {
        parseNext(seq, VariableTypeWithNewLine(state.colon.inlining))
      }
      `}` -> state.toNode(substring(state.offset, offset)) + {
        parseNext(seq, ClosingInnerBrace(state.colon.inlining))
      }
      else -> when {
        c.isValidIdentifier() -> parse(seq, state)
        else -> throw offset.failing("'$c'", "is not a valid identifier for a variable type")
      }
    }
    is VariableTypeWithSpace -> when (c) {
      null -> throw failing("Expected '$`}`$`}`', but the input has no more characters")
      ` ` -> parse(seq, state)
      `⧹r`, `⧹n` -> state.variableType.toNode(substring(state.variableType.offset, state.offset)) + {
        parseNext(seq, VariableTypeWithNewLine(state.variableType.colon.inlining))
      }
      `}` -> state.variableType.toNode(substring(state.variableType.offset, state.offset)) + {
        parseNext(seq, ClosingInnerBrace(state.variableType.colon.inlining))
      }
      else -> when {
        c.isValidIdentifier() -> parse(seq, state.variableType)
        else -> throw offset.failing("'$c'", "is not a valid identifier for a variable type")
      }
    }

    // Double-hyphen handling
    is DoubleHyphen -> when (c) {
      null -> terminalNodes(state.hyphen.literal)
      ` ` -> parse(seq, state.hyphen.literal)
      `⧹r`, `⧹n` -> parse(seq, NewLine(state.hyphen.literal))
      `{` -> ExpressionNode.Literal(substring(state.hyphen.literal.offset, state.hyphen.offset)) + {
        parseNext(seq, DoubleHyphenWithBrace)
      }
      `#` -> ExpressionNode.Literal(substring(state.hyphen.literal.offset, state.hyphen.offset)) + {
        parseNext(seq, FullLineComment)
      }
      else -> when {
        // Reserve symbols
        !c.isLetterOrDigit() ->
          throw offset.failing("Unexpected character '$c'", "following '$`-`$`-`'")
        else -> parse(seq, state.hyphen.literal)
      }
    }
    DoubleHyphenWithBrace -> when (c) {
      null -> throw failing("Expected a variable name, but the input has no more characters")
      ` `, `⧹r`, `⧹n` -> parse(seq, DoubleHyphenWithBrace)
      else -> when {
        c.isValidIdentifier() -> parse(seq, VariableName(offset, null))
        else -> throw offset.failing("'$c'", "is not a valid identifier for a variable name")
      }
    }

    // Inline literal parsing
    is InlineLiteral -> when (c) {
      null -> throw failing("Expected '$`⁆`$`}`', but the input has no more characters")
      `⁆` -> parse(seq, InlineLiteralWithBracket(offset, state.level, state))
      else -> parse(seq, state)
    }
    is InlineLiteralWithBracket -> when (c) {
      null -> throw failing("Expected '$`}`', but the input has no more characters")
      `⁆` -> parse(seq, InlineLiteralWithBracket(offset, state.inlineLiteral.level, state.inlineLiteral))
      `}` -> when (val level = state.level.previous) {
        null -> ExpressionNode.InlineLiteral(substring(state.inlineLiteral.offset, state.offset)) + {
          parseNext(seq, Literal(seq.head.index))
        }
        else -> parse(seq, InlineLiteralWithBracket(state.offset, level, state.inlineLiteral))
      }
      else -> parse(seq, state.inlineLiteral)
    }

    // Comment parsing
    InlineComment -> when (c) {
      null -> throw failing("Expected '$`#`$`}`', but the input has no more characters")
      `#` -> parse(seq, InlineCommentWithNumberSign)
      else -> parse(seq, InlineComment)
    }
    InlineCommentWithNumberSign -> when (c) {
      null -> throw failing("Expected '$`}`', but the input has no more characters")
      `#` -> parse(seq, InlineCommentWithNumberSign)
      `}` -> parse(seq, Literal(seq.head.index))
      else -> parse(seq, InlineComment)
    }
    FullLineComment -> when (c) {
      null -> throw failing("Expected '$`#`$`-`$`-`', but the input has no more characters")
      `#` -> parse(seq, FullLineCommentWithNumberSign)
      else -> parse(seq, FullLineComment)
    }
    FullLineCommentWithNumberSign -> when (c) {
      null -> throw failing("Expected '$`-`$`-`', but the input has no more characters")
      `#` -> parse(seq, FullLineCommentWithNumberSign)
      `-` -> parse(seq, FullLineCommentWithNumberSignWithHyphen)
      else -> parse(seq, FullLineComment)
    }
    FullLineCommentWithNumberSignWithHyphen -> when (c) {
      null -> throw failing("Expected '$`-`', but the input has no more characters")
      `#` -> parse(seq, FullLineCommentWithNumberSign)
      `-` -> parse(seq, ClosingOuterHyphen)
      else -> parse(seq, FullLineComment)
    }

    // Closing brace and hyphen handling
    is VariableTypeWithNewLine -> when (c) {
      null -> throw failing("Expected '$`}`', but the input has no more characters")
      ` `, `⧹r`, `⧹n` -> parse(seq, state)
      `}` -> parse(seq, ClosingInnerBrace(state.inlining))
      else -> throw offset.failing("Expected '$`}`', but '$c' was found")
    }
    is ClosingInnerBrace -> when (state.inlining) {
      null -> when (c) {
        null -> throw failing("Expected '$`-`', but the input has no more characters")
        `-` -> parse(seq, ClosingInnerHyphen)
        else -> throw offset.failing("Expected '$`-`', but '$c' was found")
      }
      else -> when (c) {
        null -> throw failing("Expected '$`}`', but the input has no more characters")
        `}` -> parse(seq, Literal(seq.head.index))
        else -> throw offset.failing("Expected '$`}`', but '$c' was found")
      }
    }
    ClosingInnerHyphen -> when (c) {
      null -> throw failing("Expected '$`-`', but the input has no more characters")
      `-` -> parse(seq, ClosingOuterHyphen)
      else -> throw offset.failing("Expected '$`-`', but '$c' was found")
    }
    ClosingOuterHyphen -> when (c) {
      null -> seqOf()
      `⧹r` -> parse(seq, ClosingCarriageReturn)
      `⧹n` -> parse(seq, NewLine(Literal(seq.head.index)))
      else -> throw offset.failing("Expected a new line, but '$c' was found")
    }
  }
}
