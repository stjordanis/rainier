package rainier.compute.asm

import rainier.compute._

private case class ExprMethodGenerator(method: MethodDef,
                                       inputs: Seq[Variable],
                                       stats: DepStats,
                                       className: String)
    extends MethodGenerator {
  val isPrivate = true
  val methodName = exprMethodName(method.sym.id)
  val methodDesc = "([D[D])D"

  private val varIndices = inputs.zipWithIndex.toMap

  traverse(method.rhs)
  ret()

  //could almost use ForEachTraverse here but the operand ordering for
  //array stores makes that not really work
  def traverse(ir: IR): Unit = {
    ir match {
      case Const(value) =>
        constant(value)
      case Parameter(variable) =>
        loadParameter(varIndices(variable))
      case BinaryIR(left, right, op) =>
        traverse(left)
        traverse(right)
        binaryOp(op)
      case UnaryIR(original, op) =>
        traverse(original)
        unaryOp(op)
      case VarDef(sym, rhs) =>
        stats.varType(sym) match {
          case Inline =>
            traverse(rhs)
          case Local(i) =>
            traverse(rhs)
            storeLocalVar(i)     
          case Global(i) =>       
            storeGlobalVar(i) {
              traverse(rhs)
            }
        }
      case VarRef(sym) =>
        stats.varType(sym) match {
          case Inline =>
            sys.error("Should not have references to inlined vars")
          case Local(i) =>
            loadLocalVar(i)
          case Global(i) =>
            loadGlobalVar(i)
        }
      case MethodRef(sym) =>
        callExprMethod(sym.id)
      case MethodDef(sym, rhs) =>
        sys.error("Should not have nested method defs")
    }
  }
}
