/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.javac.parser;

import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.tree.DocCommentTable;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotatedType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCArrayTypeTree;
import com.sun.tools.javac.tree.JCTree.JCAssert;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCErroneous;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Convert;
import com.sun.tools.javac.util.Filter;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.sun.tools.javac.parser.Tokens.TokenKind.AMP;
import static com.sun.tools.javac.parser.Tokens.TokenKind.ARROW;
import static com.sun.tools.javac.parser.Tokens.TokenKind.ASSERT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.BAR;
import static com.sun.tools.javac.parser.Tokens.TokenKind.CASE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.CATCH;
import static com.sun.tools.javac.parser.Tokens.TokenKind.CLASS;
import static com.sun.tools.javac.parser.Tokens.TokenKind.COLCOL;
import static com.sun.tools.javac.parser.Tokens.TokenKind.COLON;
import static com.sun.tools.javac.parser.Tokens.TokenKind.COMMA;
import static com.sun.tools.javac.parser.Tokens.TokenKind.DEFAULT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.DOT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.ELLIPSIS;
import static com.sun.tools.javac.parser.Tokens.TokenKind.ELSE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.ENUM;
import static com.sun.tools.javac.parser.Tokens.TokenKind.EOF;
import static com.sun.tools.javac.parser.Tokens.TokenKind.EQ;
import static com.sun.tools.javac.parser.Tokens.TokenKind.EXTENDS;
import static com.sun.tools.javac.parser.Tokens.TokenKind.FINAL;
import static com.sun.tools.javac.parser.Tokens.TokenKind.FINALLY;
import static com.sun.tools.javac.parser.Tokens.TokenKind.GT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.GTGTGTEQ;
import static com.sun.tools.javac.parser.Tokens.TokenKind.IDENTIFIER;
import static com.sun.tools.javac.parser.Tokens.TokenKind.IMPLEMENTS;
import static com.sun.tools.javac.parser.Tokens.TokenKind.IMPORT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.INSTANCEOF;
import static com.sun.tools.javac.parser.Tokens.TokenKind.INTERFACE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.INTLITERAL;
import static com.sun.tools.javac.parser.Tokens.TokenKind.LBRACE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.LBRACKET;
import static com.sun.tools.javac.parser.Tokens.TokenKind.LONGLITERAL;
import static com.sun.tools.javac.parser.Tokens.TokenKind.LPAREN;
import static com.sun.tools.javac.parser.Tokens.TokenKind.LT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.MONKEYS_AT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.NEW;
import static com.sun.tools.javac.parser.Tokens.TokenKind.PACKAGE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.PLUSEQ;
import static com.sun.tools.javac.parser.Tokens.TokenKind.PLUSPLUS;
import static com.sun.tools.javac.parser.Tokens.TokenKind.QUES;
import static com.sun.tools.javac.parser.Tokens.TokenKind.RBRACE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.RBRACKET;
import static com.sun.tools.javac.parser.Tokens.TokenKind.RPAREN;
import static com.sun.tools.javac.parser.Tokens.TokenKind.SEMI;
import static com.sun.tools.javac.parser.Tokens.TokenKind.STAR;
import static com.sun.tools.javac.parser.Tokens.TokenKind.STATIC;
import static com.sun.tools.javac.parser.Tokens.TokenKind.SUB;
import static com.sun.tools.javac.parser.Tokens.TokenKind.SUBSUB;
import static com.sun.tools.javac.parser.Tokens.TokenKind.SUPER;
import static com.sun.tools.javac.parser.Tokens.TokenKind.THIS;
import static com.sun.tools.javac.parser.Tokens.TokenKind.THROWS;
import static com.sun.tools.javac.parser.Tokens.TokenKind.TRUE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.UNDERSCORE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.VOID;
import static com.sun.tools.javac.parser.Tokens.TokenKind.WHILE;
import static com.sun.tools.javac.tree.JCTree.Tag.AND;
import static com.sun.tools.javac.tree.JCTree.Tag.BITAND;
import static com.sun.tools.javac.tree.JCTree.Tag.BITAND_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.BITOR;
import static com.sun.tools.javac.tree.JCTree.Tag.BITOR_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.BITXOR;
import static com.sun.tools.javac.tree.JCTree.Tag.BITXOR_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.COMPL;
import static com.sun.tools.javac.tree.JCTree.Tag.DIV;
import static com.sun.tools.javac.tree.JCTree.Tag.DIV_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.GE;
import static com.sun.tools.javac.tree.JCTree.Tag.IDENT;
import static com.sun.tools.javac.tree.JCTree.Tag.LE;
import static com.sun.tools.javac.tree.JCTree.Tag.LITERAL;
import static com.sun.tools.javac.tree.JCTree.Tag.MINUS;
import static com.sun.tools.javac.tree.JCTree.Tag.MINUS_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.MOD;
import static com.sun.tools.javac.tree.JCTree.Tag.MOD_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.MUL;
import static com.sun.tools.javac.tree.JCTree.Tag.MUL_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.NE;
import static com.sun.tools.javac.tree.JCTree.Tag.NEG;
import static com.sun.tools.javac.tree.JCTree.Tag.NOT;
import static com.sun.tools.javac.tree.JCTree.Tag.NO_TAG;
import static com.sun.tools.javac.tree.JCTree.Tag.OR;
import static com.sun.tools.javac.tree.JCTree.Tag.PLUS_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.POS;
import static com.sun.tools.javac.tree.JCTree.Tag.POSTDEC;
import static com.sun.tools.javac.tree.JCTree.Tag.POSTINC;
import static com.sun.tools.javac.tree.JCTree.Tag.PREDEC;
import static com.sun.tools.javac.tree.JCTree.Tag.PREINC;
import static com.sun.tools.javac.tree.JCTree.Tag.SELECT;
import static com.sun.tools.javac.tree.JCTree.Tag.SL;
import static com.sun.tools.javac.tree.JCTree.Tag.SL_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.SR;
import static com.sun.tools.javac.tree.JCTree.Tag.SR_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.TYPEAPPLY;
import static com.sun.tools.javac.tree.JCTree.Tag.TYPEARRAY;
import static com.sun.tools.javac.tree.JCTree.Tag.TYPETEST;
import static com.sun.tools.javac.tree.JCTree.Tag.USR;
import static com.sun.tools.javac.tree.JCTree.Tag.USR_ASG;
import static com.sun.tools.javac.tree.JCTree.Tag.VARDEF;

/**
 * HCZ:Parser接口的具体实现类
 *
 *  The parser maps a token sequence into an abstract syntax
 *  tree. It operates by recursive descent, with code derived
 *  systematically from an LL(1) grammar. For efficiency reasons, an
 *  operator precedence scheme is used for parsing binary operation
 *  expressions.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JavacParser implements Parser {
    /**
     * HCZ:?
     *  The number of precedence levels of infix operators.
     */
    private static final int infixPrecedenceLevels = 10;

    /**
     * HCZ:Scanner对象
     *
     *  The scanner used for lexical analysis.
     */
    protected Lexer S;

    /**
     * HCZ:语法抽象树工厂
     *
     *  The factory to be used for abstract syntax tree construction.
     */
    protected TreeMaker F;

    /**
     * HCZ:X
     *  The log to be used for error diagnostics.
     */
    private Log log;

    /**
     * HCZ:Source对象
     *
     *  The Source language setting.
     */
    private Source source;

    /**
     * HCZ：Names对象
     *  The name table.
     */
    private Names names;

    /**
     * HCZ:?
     *
     *  End position mappings container
     */
    private final AbstractEndPosTable endPosTable;

    // Because of javac's limited lookahead, some contexts are ambiguous in
    // the presence of type annotations even though they are not ambiguous
    // in the absence of type annotations.  Consider this code:
    //   void m(String [] m) { }
    //   void m(String ... m) { }
    // After parsing "String", javac calls bracketsOpt which immediately
    // returns if the next character is not '['.  Similarly, javac can see
    // if the next token is ... and in that case parse an ellipsis.  But in
    // the presence of type annotations:
    //   void m(String @A [] m) { }
    //   void m(String @A ... m) { }
    // no finite lookahead is enough to determine whether to read array
    // levels or an ellipsis.  Furthermore, if you call bracketsOpt, then
    // bracketsOpt first reads all the leading annotations and only then
    // discovers that it needs to fail.  bracketsOpt needs a way to push
    // back the extra annotations that it read.  (But, bracketsOpt should
    // not *always* be allowed to push back extra annotations that it finds
    // -- in most contexts, any such extra annotation is an error.
    //
    // The following two variables permit type annotations that have
    // already been read to be stored for later use.  Alternate
    // implementations are possible but would cause much larger changes to
    // the parser.

    /**
     * HCZ:?
     *
     *  Type annotations that have already been read but have not yet been used.
     */
    private List<JCAnnotation> typeAnnotationsPushedBack = List.nil();

    /**
     * HCZ:?
     *
     * If the parser notices extra annotations, then it either immediately
     * issues an error (if this variable is false) or places the extra
     * annotations in variable typeAnnotationsPushedBack (if this variable
     * is true).
     */
    private boolean permitTypeAnnotationsPushBack = false;

    /**
     * HCZ:?
     *
     * If the parser notices extra annotations, then it either immediately
     * issues an error (if this variable is false) or places the extra
     * annotations in variable typeAnnotationsPushedBack (if this variable
     * is true).
     */
    interface ErrorRecoveryAction {
        JCTree doRecover(JavacParser parser);
    }
    /**
     * HCZ:?
     *
     * If the parser notices extra annotations, then it either immediately
     * issues an error (if this variable is false) or places the extra
     * annotations in variable typeAnnotationsPushedBack (if this variable
     * is true).
     */
    enum BasicErrorRecoveryAction implements ErrorRecoveryAction {
        BLOCK_STMT {public JCTree doRecover(JavacParser parser) { return parser.parseStatementAsBlock(); }},
        CATCH_CLAUSE {public JCTree doRecover(JavacParser parser) { return parser.catchClause(); }}
    }

    /**
     * HCZ:构造函数
     *
     *  Construct a parser from a given scanner, tree factory and log.
     */
    protected JavacParser(ParserFactory fac,
                     Lexer S,
                     boolean keepDocComments,
                     boolean keepLineMap,
                     boolean keepEndPositions) {
        this.S = S;
        //HCZ：nextToken()-创建JavacParser对象时，得到了"package"Token对象。
        nextToken(); // prime the pump
        this.F = fac.F;
        this.log = fac.log;
        this.names = fac.names;
        this.source = fac.source;
        this.allowGenerics = source.allowGenerics();
        this.allowVarargs = source.allowVarargs();
        this.allowAsserts = source.allowAsserts();
        this.allowEnums = source.allowEnums();
        this.allowForeach = source.allowForeach();
        this.allowStaticImport = source.allowStaticImport();
        this.allowAnnotations = source.allowAnnotations();
        this.allowTWR = source.allowTryWithResources();
        this.allowDiamond = source.allowDiamond();
        this.allowMulticatch = source.allowMulticatch();
        this.allowStringFolding = fac.options.getBoolean("allowStringFolding", true);
        this.allowLambda = source.allowLambda();
        this.allowMethodReferences = source.allowMethodReferences();
        this.allowDefaultMethods = source.allowDefaultMethods();
        this.allowStaticInterfaceMethods = source.allowStaticInterfaceMethods();
        this.allowIntersectionTypesInCast = source.allowIntersectionTypesInCast();
        this.allowTypeAnnotations = source.allowTypeAnnotations();
        this.keepDocComments = keepDocComments;
        docComments = newDocCommentTable(keepDocComments, fac);
        this.keepLineMap = keepLineMap;
        this.errorTree = F.Erroneous();
        endPosTable = newEndPosTable(keepEndPositions);
    }

    /**
     * HCZ:?
     *
     * If the parser notices extra annotations, then it either immediately
     * issues an error (if this variable is false) or places the extra
     * annotations in variable typeAnnotationsPushedBack (if this variable
     * is true).
     */
    protected AbstractEndPosTable newEndPosTable(boolean keepEndPositions) {
        return  keepEndPositions
                ? new SimpleEndPosTable(this)
                : new EmptyEndPosTable(this);
    }

    /**
     * HCZ:?
     *
     * If the parser notices extra annotations, then it either immediately
     * issues an error (if this variable is false) or places the extra
     * annotations in variable typeAnnotationsPushedBack (if this variable
     * is true).
     */
    protected DocCommentTable newDocCommentTable(boolean keepDocComments, ParserFactory fac) {
        return keepDocComments ? new LazyDocCommentTable(fac) : null;
    }

    /**
     * HCZ:?
     *
     *  Switch: Should generics be recognized?
     */
    boolean allowGenerics;

    /**
     * HCZ:?
     *
     *  Switch: Should diamond operator be recognized?
     */
    boolean allowDiamond;

    /**
     * HCZ:?
     *
     *  Switch: Should multicatch clause be accepted?
     */
    boolean allowMulticatch;

    /**
     * HCZ:?
     *
     *  Switch: Should varargs be recognized?
     */
    boolean allowVarargs;

    /**
     * HCZ:?
     *
     *  Switch: should we recognize assert statements, or just give a warning?
     */
    boolean allowAsserts;

    /**
     * HCZ:?
     *
     *  Switch: should we recognize enums, or just give a warning?
     */
    boolean allowEnums;

    /**
     * HCZ:?
     *
     *  Switch: should we recognize foreach?
     */
    boolean allowForeach;

    /**
     * HCZ:?
     *
     *  Switch: should we recognize foreach?
     */
    boolean allowStaticImport;

    /**
     * HCZ:?
     *
     *  Switch: should we recognize annotations?
     */
    boolean allowAnnotations;

    /**
     * HCZ:?
     *
     *  Switch: should we recognize try-with-resources?
     */
    boolean allowTWR;

    /**
     * HCZ:?
     *
     *  Switch: should we fold strings?
     */
    boolean allowStringFolding;

    /**
     * HCZ:?
     *
     *  Switch: should we recognize lambda expressions?
     */
    boolean allowLambda;

    /**
     * HCZ:?
     *
     *  Switch: should we allow method/constructor references?
     */
    boolean allowMethodReferences;

    /**
     * HCZ:?
     *
     *  Switch: should we allow default methods in interfaces?
     */
    boolean allowDefaultMethods;

    /**
     * HCZ:?
     *
     *  Switch: should we allow static methods in interfaces?
     */
    boolean allowStaticInterfaceMethods;

    /**
     * HCZ:?
     *
     *  Switch: should we allow intersection types in cast?
     */
    boolean allowIntersectionTypesInCast;

    /**
     * HCZ:?
     *
     *  Switch: should we keep docComments?
     */
    boolean keepDocComments;

    /**
     * HCZ:?
     *
     *  Switch: should we keep line table?
     */
    boolean keepLineMap;

    /**
     * HCZ:?
     *
     *  Switch: should we recognize type annotations?
     */
    boolean allowTypeAnnotations;

    /**
     * HCZ:?
     *
     *  Switch: is "this" allowed as an identifier?
     * This is needed to parse receiver types.
     */
    boolean allowThisIdent;

    /**
     * HCZ:?
     *
     *  The type of the method receiver, as specified by a first "this" parameter.
     */
    JCVariableDecl receiverParam;


    /**
     * HCZ:?
     *
     *  When terms are parsed, the mode determines which is expected:
     *     mode = EXPR        : an expression
     *     mode = TYPE        : a type
     *     mode = NOPARAMS    : no parameters allowed for type
     *     mode = TYPEARG     : type argument
     */
    static final int EXPR = 0x1;
    /**
     * HCZ:?
     *
     *  The type of the method receiver, as specified by a first "this" parameter.
     */
    static final int TYPE = 0x2;
    /**
     * HCZ:?
     *
     *  The type of the method receiver, as specified by a first "this" parameter.
     */
    static final int NOPARAMS = 0x4;
    /**
     * HCZ:?
     *
     *  The type of the method receiver, as specified by a first "this" parameter.
     */
    static final int TYPEARG = 0x8;
    /**
     * HCZ:?
     *
     *  The type of the method receiver, as specified by a first "this" parameter.
     */
    static final int DIAMOND = 0x10;

    /**
     * HCZ:?
     *
     *  The current mode.
     */
    private int mode = 0;

    /**
     * HCZ:?
     *
     *  The mode of the term that was parsed last.
     */
    private int lastmode = 0;

    /* ---------- token management -------------- */

    /**
     * HCZ:当前的Tokens对象
     */
    protected Token token;
    /**
     * HCZ:当前的Tokens对象
     */
    public Token token() {
        return token;
    }

    /**
     * HCZ:Scanner对象向下走一个Token
     */
    public void nextToken() {
        //HCZ:Scanner对象向下走一个Token
        S.nextToken();
        //HCZ:记录下当前遍历到的Token对象
        token = S.token();
    }

    /**
     * HCZ:?
     */
    protected boolean peekToken(Filter<TokenKind> tk) {
        return peekToken(0, tk);
    }
    /**
     * HCZ:?
     */
    protected boolean peekToken(int lookahead, Filter<TokenKind> tk) {
        return tk.accepts(S.token(lookahead + 1).kind);
    }
    /**
     * HCZ:X
     */
    protected boolean peekToken(Filter<TokenKind> tk1, Filter<TokenKind> tk2) {
        return peekToken(0, tk1, tk2);
    }
    /**
     * HCZ:?
     */
    protected boolean peekToken(int lookahead, Filter<TokenKind> tk1, Filter<TokenKind> tk2) {
        return tk1.accepts(S.token(lookahead + 1).kind) &&
                tk2.accepts(S.token(lookahead + 2).kind);
    }
    /**
     * HCZ:X
     */
    protected boolean peekToken(Filter<TokenKind> tk1, Filter<TokenKind> tk2, Filter<TokenKind> tk3) {
        return peekToken(0, tk1, tk2, tk3);
    }
    /**
     * HCZ:?
     */
    protected boolean peekToken(int lookahead, Filter<TokenKind> tk1, Filter<TokenKind> tk2, Filter<TokenKind> tk3) {
        return tk1.accepts(S.token(lookahead + 1).kind) &&
                tk2.accepts(S.token(lookahead + 2).kind) &&
                tk3.accepts(S.token(lookahead + 3).kind);
    }
    /**
     * HCZ:X
     */
    @SuppressWarnings("unchecked")
    protected boolean peekToken(Filter<TokenKind>... kinds) {
        return peekToken(0, kinds);
    }

    @SuppressWarnings("unchecked")
    protected boolean peekToken(int lookahead, Filter<TokenKind>... kinds) {
        for (; lookahead < kinds.length ; lookahead++) {
            if (!kinds[lookahead].accepts(S.token(lookahead + 1).kind)) {
                return false;
            }
        }
        return true;
    }

    /* ---------- error recovery -------------- */
    /**
     * HCZ:?
     */
    private JCErroneous errorTree;

    /**
     * HCZ:?
     *
     *  Skip forward until a suitable stop token is found.
     */
    private void skip(boolean stopAtImport, boolean stopAtMemberDecl, boolean stopAtIdentifier, boolean stopAtStatement) {
         while (true) {
             switch (token.kind) {
                 //HCZ：nextToken()-处理";"标识符
                case SEMI:
                    nextToken();
                    return;
                case PUBLIC:
                case FINAL:
                case ABSTRACT:
                case MONKEYS_AT:
                case EOF:
                case CLASS:
                case INTERFACE:
                case ENUM:
                    return;
                case IMPORT:
                    if (stopAtImport)
                        return;
                    break;
                case LBRACE:
                case RBRACE:
                case PRIVATE:
                case PROTECTED:
                case STATIC:
                case TRANSIENT:
                case NATIVE:
                case VOLATILE:
                case SYNCHRONIZED:
                case STRICTFP:
                case LT:
                case BYTE:
                case SHORT:
                case CHAR:
                case INT:
                case LONG:
                case FLOAT:
                case DOUBLE:
                case BOOLEAN:
                case VOID:
                    if (stopAtMemberDecl)
                        return;
                    break;
                case UNDERSCORE:
                case IDENTIFIER:
                   if (stopAtIdentifier)
                        return;
                    break;
                case CASE:
                case DEFAULT:
                case IF:
                case FOR:
                case WHILE:
                case DO:
                case TRY:
                case SWITCH:
                case RETURN:
                case THROW:
                case BREAK:
                case CONTINUE:
                case ELSE:
                case FINALLY:
                case CATCH:
                    if (stopAtStatement)
                        return;
                    break;
            }
            //HCZ：nextToken()-？
            nextToken();
        }
    }

    /**
     * HCZ:?
     */
    private JCErroneous syntaxError(int pos, String key, TokenKind... args) {
        return syntaxError(pos, List.<JCTree>nil(), key, args);
    }

    /**
     * HCZ:?
     */
    private JCErroneous syntaxError(int pos, List<JCTree> errs, String key, TokenKind... args) {
        setErrorEndPos(pos);
        JCErroneous err = F.at(pos).Erroneous(errs);
        reportSyntaxError(err, key, (Object[])args);
        if (errs != null) {
            JCTree last = errs.last();
            if (last != null)
                storeEnd(last, pos);
        }
        return toP(err);
    }

    /**
     * HCZ:?
     */
    private int errorPos = Position.NOPOS;

    /**
     * HCZ:?
     *
     * Report a syntax using the given the position parameter and arguments,
     * unless one was already reported at the same position.
     */
    private void reportSyntaxError(int pos, String key, Object... args) {
        JCDiagnostic.DiagnosticPosition diag = new JCDiagnostic.SimpleDiagnosticPosition(pos);
        reportSyntaxError(diag, key, args);
    }

    /**
     * HCZ:?
     *
     * Report a syntax error using the given DiagnosticPosition object and
     * arguments, unless one was already reported at the same position.
     */
    private void reportSyntaxError(JCDiagnostic.DiagnosticPosition diagPos, String key, Object... args) {
        int pos = diagPos.getPreferredPosition();
        if (pos > S.errPos() || pos == Position.NOPOS) {
            if (token.kind == EOF) {
                error(diagPos, "premature.eof");
            } else {
                error(diagPos, key, args);
            }
        }
        S.errPos(pos);
        if (token.pos == errorPos)
            //HCZ：nextToken()-
            nextToken(); // guarantee progress
        errorPos = token.pos;
    }


    /**
     * HCZ:?
     *
     * Generate a syntax error at current position unless one was already
     *  reported at the same position.
     */
    private JCErroneous syntaxError(String key) {
        return syntaxError(token.pos, key);
    }

    /**
     * HCZ:?
     *
     *  Generate a syntax error at current position unless one was
     *  already reported at the same position.
     */
    private JCErroneous syntaxError(String key, TokenKind arg) {
        return syntaxError(token.pos, key, arg);
    }

    /**
     * HCZ:?
     *
     *  If next input token matches given token, skip it, otherwise report
     *  an error.
     */
    public void accept(TokenKind tk) {
        if (token.kind == tk) {
            //HCZ：nextToken()
            nextToken();
        } else {
            setErrorEndPos(token.pos);
            reportSyntaxError(S.prevToken().endPos, "expected", tk);
        }
    }

    /**
     * HCZ:?
     *
     * Report an illegal start of expression/type error at given position.
     */
    JCExpression illegal(int pos) {
        setErrorEndPos(pos);
        if ((mode & EXPR) != 0)
            return syntaxError(pos, "illegal.start.of.expr");
        else
            return syntaxError(pos, "illegal.start.of.type");

    }

    /**
     * HCZ:?
     *
     *  Report an illegal start of expression/type error at current position.
     */
    JCExpression illegal() {
        return illegal(token.pos);
    }

    /**
     * HCZ:?
     *
     *  Diagnose a modifier flag from the set, if any. */
    void checkNoMods(long mods) {
        if (mods != 0) {
            long lowestMod = mods & -mods;
            error(token.pos, "mod.not.allowed.here",
                      Flags.asFlagSet(lowestMod));
        }
    }

/* ---------- doc comments --------- */

    /**
     * HCZ:?
     *
     *  A table to store all documentation comments
     *  indexed by the tree nodes they refer to.
     *  defined only if option flag keepDocComment is set.
     */
    private final DocCommentTable docComments;

    /**
     * HCZ:?
     *
     *  Make an entry into docComments hashtable,
     *  provided flag keepDocComments is set and given doc comment is non-null.
     *  @param tree   The tree to be used as index in the hashtable
     *  @param dc     The doc comment to associate with the tree, or null.
     */
    void attach(JCTree tree, Comment dc) {
        if (keepDocComments && dc != null) {
//          System.out.println("doc comment = ");System.out.println(dc);//DEBUG
            docComments.putComment(tree, dc);
        }
    }

/* -------- source positions ------- */

    /**
     * HCZ:?
     */
    private void setErrorEndPos(int errPos) {
        endPosTable.setErrorEndPos(errPos);
    }

    /**
     * HCZ:?
     */
    private void storeEnd(JCTree tree, int endpos) {
        endPosTable.storeEnd(tree, endpos);
    }

    /**
     * HCZ:?
     */
    private <T extends JCTree> T to(T t) {
        return endPosTable.to(t);
    }

    /**
     * HCZ:?
     */
    private <T extends JCTree> T toP(T t) {
        return endPosTable.toP(t);
    }

    /**
     * HCZ:?
     *
     *  Get the start position for a tree node.  The start position is
     * defined to be the position of the first character of the first
     * token of the node's source text.
     * @param tree  The tree node
     */
    public int getStartPos(JCTree tree) {
        return TreeInfo.getStartPos(tree);
    }

    /**
     * HCZ:?
     *
     * Get the end position for a tree node.  The end position is
     * defined to be the position of the last character of the last
     * token of the node's source text.  Returns Position.NOPOS if end
     * positions are not generated or the position is otherwise not
     * found.
     * @param tree  The tree node
     */
    public int getEndPos(JCTree tree) {
        return endPosTable.getEndPos(tree);
    }



/* ---------- parsing -------------- */

    /**
     * HCZ:调用ident()，会获取当前为标识符的Token对象的下一个符号Token对象
     *
     * Ident = IDENTIFIER
     */
    Name ident() {
        if (token.kind == IDENTIFIER) {
            Name name = token.name();
            //HCZ：nextToken()-如果当前Token对象是"标识符"，则取下一个Token对象(可能是符号，如：当前是"包名1"，则下一个是".")
            nextToken();
            return name;
        } else if (token.kind == ASSERT) {
            if (allowAsserts) {
                error(token.pos, "assert.as.identifier");
                //HCZ：nextToken()-？
                nextToken();
                return names.error;
            } else {
                warning(token.pos, "assert.as.identifier");
                Name name = token.name();
                //HCZ：nextToken()-？
                nextToken();
                return name;
            }
        } else if (token.kind == ENUM) {
            if (allowEnums) {
                error(token.pos, "enum.as.identifier");
                //HCZ：nextToken()-？
                nextToken();
                return names.error;
            } else {
                warning(token.pos, "enum.as.identifier");
                Name name = token.name();
                //HCZ：nextToken()-？
                nextToken();
                return name;
            }
        } else if (token.kind == THIS) {
            if (allowThisIdent) {
                // Make sure we're using a supported source version.
                checkTypeAnnotations();
                Name name = token.name();
                //HCZ：nextToken()-？
                nextToken();
                return name;
            } else {
                error(token.pos, "this.as.identifier");
                //HCZ：nextToken()-？
                nextToken();
                return names.error;
            }
        } else if (token.kind == UNDERSCORE) {
            warning(token.pos, "underscore.as.identifier");
            Name name = token.name();
            //HCZ：nextToken()-？
            nextToken();
            return name;
        } else {
            accept(IDENTIFIER);
            return names.error;
        }
    }

    /**
     * HCZ:如果当前处理的Token对象是"."，会不断调用
     *
     * Qualident = Ident { DOT [Annotations] Ident }
     */
    public JCExpression qualident(boolean allowAnnos) {
        //HCZ：调用ident()，会获取当前为标识符的Token对象的下一个符号Token对象
        JCExpression t = toP(F.at(token.pos).Ident(ident()));
        //HCZ：如果是当前Token对象是"."，则
        while (token.kind == DOT) {
            int pos = token.pos;
            //HCZ：nextToken()-如果当前Token对象是"."，则获取下一个标识符的Token对象。(如：包名1.包名2，当前Token对象是"."，下一个Token对象是"包名2")
            nextToken();
            List<JCAnnotation> tyannos = null;
            if (allowAnnos) {
                tyannos = typeAnnotationsOpt();
            }
            t = toP(F.at(pos).Select(t, ident()));
            if (tyannos != null && tyannos.nonEmpty()) {
                t = toP(F.at(tyannos.head.pos).AnnotatedType(tyannos, t));
            }
        }
        //HCZ：？返回完整的包名
        return t;
    }

    /**
     * HCZ:?
     */
    JCExpression literal(Name prefix) {
        return literal(prefix, token.pos);
    }

    /**
     * HCZ:?
     *
     * Literal =
     *     INTLITERAL
     *   | LONGLITERAL
     *   | FLOATLITERAL
     *   | DOUBLELITERAL
     *   | CHARLITERAL
     *   | STRINGLITERAL
     *   | TRUE
     *   | FALSE
     *   | NULL
     */
    JCExpression literal(Name prefix, int pos) {
        JCExpression t = errorTree;
        switch (token.kind) {
        case INTLITERAL:
            try {
                t = F.at(pos).Literal(
                    TypeTag.INT,
                    Convert.string2int(strval(prefix), token.radix()));
            } catch (NumberFormatException ex) {
                error(token.pos, "int.number.too.large", strval(prefix));
            }
            break;
        case LONGLITERAL:
            try {
                t = F.at(pos).Literal(
                    TypeTag.LONG,
                    new Long(Convert.string2long(strval(prefix), token.radix())));
            } catch (NumberFormatException ex) {
                error(token.pos, "int.number.too.large", strval(prefix));
            }
            break;
        case FLOATLITERAL: {
            String proper = token.radix() == 16 ?
                    ("0x"+ token.stringVal()) :
                    token.stringVal();
            Float n;
            try {
                n = Float.valueOf(proper);
            } catch (NumberFormatException ex) {
                // error already reported in scanner
                n = Float.NaN;
            }
            if (n.floatValue() == 0.0f && !isZero(proper))
                error(token.pos, "fp.number.too.small");
            else if (n.floatValue() == Float.POSITIVE_INFINITY)
                error(token.pos, "fp.number.too.large");
            else
                t = F.at(pos).Literal(TypeTag.FLOAT, n);
            break;
        }
        case DOUBLELITERAL: {
            String proper = token.radix() == 16 ?
                    ("0x"+ token.stringVal()) :
                    token.stringVal();
            Double n;
            try {
                n = Double.valueOf(proper);
            } catch (NumberFormatException ex) {
                // error already reported in scanner
                n = Double.NaN;
            }
            if (n.doubleValue() == 0.0d && !isZero(proper))
                error(token.pos, "fp.number.too.small");
            else if (n.doubleValue() == Double.POSITIVE_INFINITY)
                error(token.pos, "fp.number.too.large");
            else
                t = F.at(pos).Literal(TypeTag.DOUBLE, n);
            break;
        }
        case CHARLITERAL:
            t = F.at(pos).Literal(
                TypeTag.CHAR,
                token.stringVal().charAt(0) + 0);
            break;
        case STRINGLITERAL:
            t = F.at(pos).Literal(
                TypeTag.CLASS,
                token.stringVal());
            break;
        case TRUE: case FALSE:
            t = F.at(pos).Literal(
                TypeTag.BOOLEAN,
                (token.kind == TRUE ? 1 : 0));
            break;
        case NULL:
            t = F.at(pos).Literal(
                TypeTag.BOT,
                null);
            break;
        default:
            Assert.error();
        }
        if (t == errorTree)
            t = F.at(pos).Erroneous();
        storeEnd(t, token.endPos);
        //HCZ：nextToken()-？
        nextToken();
        return t;
    }
    //where  HCZ:?
        boolean isZero(String s) {
            char[] cs = s.toCharArray();
            int base = ((cs.length > 1 && Character.toLowerCase(cs[1]) == 'x') ? 16 : 10);
            int i = ((base==16) ? 2 : 0);
            while (i < cs.length && (cs[i] == '0' || cs[i] == '.')) i++;
            return !(i < cs.length && (Character.digit(cs[i], base) > 0));
        }
        /**
         * HCZ:?
         */
        String strval(Name prefix) {
            String s = token.stringVal();
            return prefix.isEmpty() ? s : prefix + s;
        }

    /**
     * HCZ:?
     *
     * terms can be either expressions or types.
     */
    public JCExpression parseExpression() {
        return term(EXPR);
    }

    /**
     * HCZ:?
     *
     * parses (optional) type annotations followed by a type. If the
     * annotations are present before the type and are not consumed during array
     * parsing, this method returns a {@link JCAnnotatedType} consisting of
     * these annotations and the underlying type. Otherwise, it returns the
     * underlying type.
     *
     * <p>
     *
     * Note that this method sets {@code mode} to {@code TYPE} first, before
     * parsing annotations.
     */
    public JCExpression parseType() {
        List<JCAnnotation> annotations = typeAnnotationsOpt();
        return parseType(annotations);
    }

    /**
     * HCZ:?
     */
    public JCExpression parseType(List<JCAnnotation> annotations) {
        JCExpression result = unannotatedType();

        if (annotations.nonEmpty()) {
            result = insertAnnotationsToMostInner(result, annotations, false);
        }

        return result;
    }

    /**
     * HCZ:?
     */
    public JCExpression unannotatedType() {
        return term(TYPE);
    }

    /**
     * HCZ:?
     */
    JCExpression term(int newmode) {
        int prevmode = mode;
        mode = newmode;
        JCExpression t = term();
        lastmode = mode;
        mode = prevmode;
        return t;
    }

    /**
     * HCZ:?
     *
     *  {@literal
     *  Expression = Expression1 [ExpressionRest]
     *  ExpressionRest = [AssignmentOperator Expression1]
     *  AssignmentOperator = "=" | "+=" | "-=" | "*=" | "/=" |
     *                       "&=" | "|=" | "^=" |
     *                       "%=" | "<<=" | ">>=" | ">>>="
     *  Type = Type1
     *  TypeNoParams = TypeNoParams1
     *  StatementExpression = Expression
     *  ConstantExpression = Expression
     *  }
     */
    JCExpression term() {
        JCExpression t = term1();
        if ((mode & EXPR) != 0 &&
            token.kind == EQ || PLUSEQ.compareTo(token.kind) <= 0 && token.kind.compareTo(GTGTGTEQ) <= 0)
            return termRest(t);
        else
            return t;
    }

    /**
     * HCZ:?
     */
    JCExpression termRest(JCExpression t) {
        switch (token.kind) {
        case EQ: {
            int pos = token.pos;
            //HCZ：nextToken()-？
            nextToken();
            mode = EXPR;
            JCExpression t1 = term();
            return toP(F.at(pos).Assign(t, t1));
        }
        case PLUSEQ:
        case SUBEQ:
        case STAREQ:
        case SLASHEQ:
        case PERCENTEQ:
        case AMPEQ:
        case BAREQ:
        case CARETEQ:
        case LTLTEQ:
        case GTGTEQ:
        case GTGTGTEQ:
            int pos = token.pos;
            TokenKind tk = token.kind;
            //HCZ：nextToken()-？
            nextToken();
            mode = EXPR;
            JCExpression t1 = term();
            return F.at(pos).Assignop(optag(tk), t, t1);
        default:
            return t;
        }
    }

    /**
     * HCZ:?
     *
     *  Expression1   = Expression2 [Expression1Rest]
     *  Type1         = Type2
     *  TypeNoParams1 = TypeNoParams2
     */
    JCExpression term1() {
        JCExpression t = term2();
        if ((mode & EXPR) != 0 && token.kind == QUES) {
            mode = EXPR;
            return term1Rest(t);
        } else {
            return t;
        }
    }

    /**
     * HCZ:?
     *
     * Expression1Rest = ["?" Expression ":" Expression1]
     */
    JCExpression term1Rest(JCExpression t) {
        if (token.kind == QUES) {
            int pos = token.pos;
            //HCZ：nextToken()-？
            nextToken();
            JCExpression t1 = term();
            accept(COLON);
            JCExpression t2 = term1();
            return F.at(pos).Conditional(t, t1, t2);
        } else {
            return t;
        }
    }

    /**
     * HCZ:?
     *
     *  Expression2   = Expression3 [Expression2Rest]
     *  Type2         = Type3
     *  TypeNoParams2 = TypeNoParams3
     */
    JCExpression term2() {
        JCExpression t = term3();
        if ((mode & EXPR) != 0 && prec(token.kind) >= TreeInfo.orPrec) {
            mode = EXPR;
            return term2Rest(t, TreeInfo.orPrec);
        } else {
            return t;
        }
    }

    /**
     * HCZ:?
     *
     *  Expression2Rest = {infixop Expression3}
     *                  | Expression3 instanceof Type
     *  infixop         = "||"
     *                  | "&&"
     *                  | "|"
     *                  | "^"
     *                  | "&"
     *                  | "==" | "!="
     *                  | "<" | ">" | "<=" | ">="
     *                  | "<<" | ">>" | ">>>"
     *                  | "+" | "-"
     *                  | "*" | "/" | "%"
     */
    JCExpression term2Rest(JCExpression t, int minprec) {
        JCExpression[] odStack = newOdStack();
        Token[] opStack = newOpStack();

        // optimization, was odStack = new Tree[...]; opStack = new Tree[...];
        int top = 0;
        odStack[0] = t;
        int startPos = token.pos;
        Token topOp = Tokens.DUMMY;
        while (prec(token.kind) >= minprec) {
            opStack[top] = topOp;
            top++;
            topOp = token;
            //HCZ：nextToken()-？
            nextToken();
            odStack[top] = (topOp.kind == INSTANCEOF) ? parseType() : term3();
            while (top > 0 && prec(topOp.kind) >= prec(token.kind)) {
                odStack[top-1] = makeOp(topOp.pos, topOp.kind, odStack[top-1],
                                        odStack[top]);
                top--;
                topOp = opStack[top];
            }
        }
        Assert.check(top == 0);
        t = odStack[0];

        if (t.hasTag(JCTree.Tag.PLUS)) {
            StringBuilder buf = foldStrings(t);
            if (buf != null) {
                t = toP(F.at(startPos).Literal(TypeTag.CLASS, buf.toString()));
            }
        }

        odStackSupply.add(odStack);
        opStackSupply.add(opStack);
        return t;
    }
    //where
        /**
         * HCZ:?
         *
         *  Construct a binary or type test node.
         */
        private JCExpression makeOp(int pos,
                                    TokenKind topOp,
                                    JCExpression od1,
                                    JCExpression od2)
        {
            if (topOp == INSTANCEOF) {
                return F.at(pos).TypeTest(od1, od2);
            } else {
                return F.at(pos).Binary(optag(topOp), od1, od2);
            }
        }
        /**
         * HCZ:?
         *
         *  If tree is a concatenation of string literals, replace it
         *  by a single literal representing the concatenated string.
         */
        protected StringBuilder foldStrings(JCTree tree) {
            if (!allowStringFolding)
                return null;
            List<String> buf = List.nil();
            while (true) {
                if (tree.hasTag(LITERAL)) {
                    JCLiteral lit = (JCLiteral) tree;
                    if (lit.typetag == TypeTag.CLASS) {
                        StringBuilder sbuf =
                            new StringBuilder((String)lit.value);
                        while (buf.nonEmpty()) {
                            sbuf.append(buf.head);
                            buf = buf.tail;
                        }
                        return sbuf;
                    }
                } else if (tree.hasTag(JCTree.Tag.PLUS)) {
                    JCBinary op = (JCBinary)tree;
                    if (op.rhs.hasTag(LITERAL)) {
                        JCLiteral lit = (JCLiteral) op.rhs;
                        if (lit.typetag == TypeTag.CLASS) {
                            buf = buf.prepend((String) lit.value);
                            tree = op.lhs;
                            continue;
                        }
                    }
                }
                return null;
            }
        }

        /**
         * HCZ:?
         *
         *  optimization: To save allocating a new operand/operator stack
         *  for every binary operation, we use supplys.
         */
        ArrayList<JCExpression[]> odStackSupply = new ArrayList<JCExpression[]>();
        /**
         * HCZ:?
         */
        ArrayList<Token[]> opStackSupply = new ArrayList<Token[]>();
        /**
         * HCZ:?
         */
        private JCExpression[] newOdStack() {
            if (odStackSupply.isEmpty())
                return new JCExpression[infixPrecedenceLevels + 1];
            return odStackSupply.remove(odStackSupply.size() - 1);
        }
        /**
         * HCZ:?
         */
        private Token[] newOpStack() {
            if (opStackSupply.isEmpty())
                return new Token[infixPrecedenceLevels + 1];
            return opStackSupply.remove(opStackSupply.size() - 1);
        }

    /**
     * HCZ:?
     *
     *  Expression3    = PrefixOp Expression3
     *                 | "(" Expr | TypeNoParams ")" Expression3
     *                 | Primary {Selector} {PostfixOp}
     *
     *  {@literal
     *  Primary        = "(" Expression ")"
     *                 | Literal
     *                 | [TypeArguments] THIS [Arguments]
     *                 | [TypeArguments] SUPER SuperSuffix
     *                 | NEW [TypeArguments] Creator
     *                 | "(" Arguments ")" "->" ( Expression | Block )
     *                 | Ident "->" ( Expression | Block )
     *                 | [Annotations] Ident { "." [Annotations] Ident }
     *                 | Expression3 MemberReferenceSuffix
     *                   [ [Annotations] "[" ( "]" BracketsOpt "." CLASS | Expression "]" )
     *                   | Arguments
     *                   | "." ( CLASS | THIS | [TypeArguments] SUPER Arguments | NEW [TypeArguments] InnerCreator )
     *                   ]
     *                 | BasicType BracketsOpt "." CLASS
     *  }
     *
     *  PrefixOp       = "++" | "--" | "!" | "~" | "+" | "-"
     *  PostfixOp      = "++" | "--"
     *  Type3          = Ident { "." Ident } [TypeArguments] {TypeSelector} BracketsOpt
     *                 | BasicType
     *  TypeNoParams3  = Ident { "." Ident } BracketsOpt
     *  Selector       = "." [TypeArguments] Ident [Arguments]
     *                 | "." THIS
     *                 | "." [TypeArguments] SUPER SuperSuffix
     *                 | "." NEW [TypeArguments] InnerCreator
     *                 | "[" Expression "]"
     *  TypeSelector   = "." Ident [TypeArguments]
     *  SuperSuffix    = Arguments | "." Ident [Arguments]
     */
    protected JCExpression term3() {
        int pos = token.pos;
        JCExpression t;
        List<JCExpression> typeArgs = typeArgumentsOpt(EXPR);
        switch (token.kind) {
        case QUES:
            if ((mode & TYPE) != 0 && (mode & (TYPEARG|NOPARAMS)) == TYPEARG) {
                mode = TYPE;
                return typeArgument();
            } else
                return illegal();
        case PLUSPLUS: case SUBSUB: case BANG: case TILDE: case PLUS: case SUB:
            if (typeArgs == null && (mode & EXPR) != 0) {
                TokenKind tk = token.kind;
                //HCZ：nextToken()-？
                nextToken();
                mode = EXPR;
                if (tk == SUB &&
                    (token.kind == INTLITERAL || token.kind == LONGLITERAL) &&
                    token.radix() == 10) {
                    mode = EXPR;
                    t = literal(names.hyphen, pos);
                } else {
                    t = term3();
                    return F.at(pos).Unary(unoptag(tk), t);
                }
            } else return illegal();
            break;
        case LPAREN:
            if (typeArgs == null && (mode & EXPR) != 0) {
                ParensResult pres = analyzeParens();
                switch (pres) {
                    case CAST:
                       accept(LPAREN);
                       mode = TYPE;
                       int pos1 = pos;
                       List<JCExpression> targets = List.of(t = term3());
                       while (token.kind == AMP) {
                           checkIntersectionTypesInCast();
                           accept(AMP);
                           targets = targets.prepend(term3());
                       }
                       if (targets.length() > 1) {
                           t = toP(F.at(pos1).TypeIntersection(targets.reverse()));
                       }
                       accept(RPAREN);
                       mode = EXPR;
                       JCExpression t1 = term3();
                       return F.at(pos).TypeCast(t, t1);
                    case IMPLICIT_LAMBDA:
                    case EXPLICIT_LAMBDA:
                        t = lambdaExpressionOrStatement(true, pres == ParensResult.EXPLICIT_LAMBDA, pos);
                        break;
                    default: //PARENS
                        accept(LPAREN);
                        mode = EXPR;
                        t = termRest(term1Rest(term2Rest(term3(), TreeInfo.orPrec)));
                        accept(RPAREN);
                        t = toP(F.at(pos).Parens(t));
                        break;
                }
            } else {
                return illegal();
            }
            break;
        case THIS:
            if ((mode & EXPR) != 0) {
                mode = EXPR;
                t = to(F.at(pos).Ident(names._this));
                //HCZ：nextToken()-？
                nextToken();
                if (typeArgs == null)
                    t = argumentsOpt(null, t);
                else
                    t = arguments(typeArgs, t);
                typeArgs = null;
            } else return illegal();
            break;
        case SUPER:
            if ((mode & EXPR) != 0) {
                mode = EXPR;
                t = to(F.at(pos).Ident(names._super));
                t = superSuffix(typeArgs, t);
                typeArgs = null;
            } else return illegal();
            break;
        case INTLITERAL: case LONGLITERAL: case FLOATLITERAL: case DOUBLELITERAL:
        case CHARLITERAL: case STRINGLITERAL:
        case TRUE: case FALSE: case NULL:
            if (typeArgs == null && (mode & EXPR) != 0) {
                mode = EXPR;
                t = literal(names.empty);
            } else return illegal();
            break;
        case NEW:
            if (typeArgs != null) return illegal();
            if ((mode & EXPR) != 0) {
                mode = EXPR;
                //HCZ：nextToken()-？
                nextToken();
                if (token.kind == LT) typeArgs = typeArguments(false);
                t = creator(pos, typeArgs);
                typeArgs = null;
            } else return illegal();
            break;
        case MONKEYS_AT:
            // Only annotated cast types and method references are valid
            List<JCAnnotation> typeAnnos = typeAnnotationsOpt();
            if (typeAnnos.isEmpty()) {
                // else there would be no '@'
                throw new AssertionError("Expected type annotations, but found none!");
            }

            JCExpression expr = term3();

            if ((mode & TYPE) == 0) {
                // Type annotations on class literals no longer legal
                switch (expr.getTag()) {
                case REFERENCE: {
                    JCMemberReference mref = (JCMemberReference) expr;
                    mref.expr = toP(F.at(pos).AnnotatedType(typeAnnos, mref.expr));
                    t = mref;
                    break;
                }
                case SELECT: {
                    JCFieldAccess sel = (JCFieldAccess) expr;

                    if (sel.name != names._class) {
                        return illegal();
                    } else {
                        log.error(token.pos, "no.annotations.on.dot.class");
                        return expr;
                    }
                }
                default:
                    return illegal(typeAnnos.head.pos);
                }

            } else {
                // Type annotations targeting a cast
                t = insertAnnotationsToMostInner(expr, typeAnnos, false);
            }
            break;
        case UNDERSCORE: case IDENTIFIER: case ASSERT: case ENUM:
            if (typeArgs != null) return illegal();
            if ((mode & EXPR) != 0 && peekToken(ARROW)) {
                t = lambdaExpressionOrStatement(false, false, pos);
            } else {
                t = toP(F.at(token.pos).Ident(ident()));
                loop: while (true) {
                    pos = token.pos;
                    final List<JCAnnotation> annos = typeAnnotationsOpt();

                    // need to report an error later if LBRACKET is for array
                    // index access rather than array creation level
                    if (!annos.isEmpty() && token.kind != LBRACKET && token.kind != ELLIPSIS)
                        return illegal(annos.head.pos);

                    switch (token.kind) {
                    case LBRACKET:
                        //HCZ：nextToken()-？
                        nextToken();
                        if (token.kind == RBRACKET) {
                            //HCZ：nextToken()-？
                            nextToken();
                            t = bracketsOpt(t);
                            t = toP(F.at(pos).TypeArray(t));
                            if (annos.nonEmpty()) {
                                t = toP(F.at(pos).AnnotatedType(annos, t));
                            }
                            // .class is only allowed if there were no annotations
                            JCExpression nt = bracketsSuffix(t);
                            if (nt != t && (annos.nonEmpty() || TreeInfo.containsTypeAnnotation(t))) {
                                // t and nt are different if bracketsSuffix parsed a .class.
                                // The check for nonEmpty covers the case when the whole array is annotated.
                                // Helper method isAnnotated looks for annos deeply within t.
                                syntaxError("no.annotations.on.dot.class");
                            }
                            t = nt;
                        } else {
                            if ((mode & EXPR) != 0) {
                                mode = EXPR;
                                JCExpression t1 = term();
                                if (!annos.isEmpty()) t = illegal(annos.head.pos);
                                t = to(F.at(pos).Indexed(t, t1));
                            }
                            accept(RBRACKET);
                        }
                        break loop;
                    case LPAREN:
                        if ((mode & EXPR) != 0) {
                            mode = EXPR;
                            t = arguments(typeArgs, t);
                            if (!annos.isEmpty()) t = illegal(annos.head.pos);
                            typeArgs = null;
                        }
                        break loop;
                    case DOT:
                        //HCZ：nextToken()-？
                        nextToken();
                        int oldmode = mode;
                        mode &= ~NOPARAMS;
                        typeArgs = typeArgumentsOpt(EXPR);
                        mode = oldmode;
                        if ((mode & EXPR) != 0) {
                            switch (token.kind) {
                            case CLASS:
                                if (typeArgs != null) return illegal();
                                mode = EXPR;
                                t = to(F.at(pos).Select(t, names._class));
                                //HCZ：nextToken()-？
                                nextToken();
                                break loop;
                            case THIS:
                                if (typeArgs != null) return illegal();
                                mode = EXPR;
                                t = to(F.at(pos).Select(t, names._this));
                                //HCZ：nextToken()-？
                                nextToken();
                                break loop;
                            case SUPER:
                                mode = EXPR;
                                t = to(F.at(pos).Select(t, names._super));
                                t = superSuffix(typeArgs, t);
                                typeArgs = null;
                                break loop;
                            case NEW:
                                if (typeArgs != null) return illegal();
                                mode = EXPR;
                                int pos1 = token.pos;
                                //HCZ：nextToken()-？
                                nextToken();
                                if (token.kind == LT) typeArgs = typeArguments(false);
                                t = innerCreator(pos1, typeArgs, t);
                                typeArgs = null;
                                break loop;
                            }
                        }

                        List<JCAnnotation> tyannos = null;
                        if ((mode & TYPE) != 0 && token.kind == MONKEYS_AT) {
                            tyannos = typeAnnotationsOpt();
                        }
                        // typeArgs saved for next loop iteration.
                        t = toP(F.at(pos).Select(t, ident()));
                        if (tyannos != null && tyannos.nonEmpty()) {
                            t = toP(F.at(tyannos.head.pos).AnnotatedType(tyannos, t));
                        }
                        break;
                    case ELLIPSIS:
                        if (this.permitTypeAnnotationsPushBack) {
                            this.typeAnnotationsPushedBack = annos;
                        } else if (annos.nonEmpty()) {
                            // Don't return here -- error recovery attempt
                            illegal(annos.head.pos);
                        }
                        break loop;
                    case LT:
                        if ((mode & TYPE) == 0 && isUnboundMemberRef()) {
                            //this is an unbound method reference whose qualifier
                            //is a generic type i.e. A<S>::m
                            int pos1 = token.pos;
                            accept(LT);
                            ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();
                            args.append(typeArgument());
                            while (token.kind == COMMA) {
                                //HCZ：nextToken()-？
                                nextToken();
                                args.append(typeArgument());
                            }
                            accept(GT);
                            t = toP(F.at(pos1).TypeApply(t, args.toList()));
                            checkGenerics();
                            while (token.kind == DOT) {
                                //HCZ：nextToken()-？
                                nextToken();
                                mode = TYPE;
                                t = toP(F.at(token.pos).Select(t, ident()));
                                t = typeArgumentsOpt(t);
                            }
                            t = bracketsOpt(t);
                            if (token.kind != COLCOL) {
                                //method reference expected here
                                t = illegal();
                            }
                            mode = EXPR;
                            return term3Rest(t, typeArgs);
                        }
                        break loop;
                    default:
                        break loop;
                    }
                }
            }
            if (typeArgs != null) illegal();
            t = typeArgumentsOpt(t);
            break;
        case BYTE: case SHORT: case CHAR: case INT: case LONG: case FLOAT:
        case DOUBLE: case BOOLEAN:
            if (typeArgs != null) illegal();
            t = bracketsSuffix(bracketsOpt(basicType()));
            break;
        case VOID:
            if (typeArgs != null) illegal();
            if ((mode & EXPR) != 0) {
                //HCZ：nextToken()-？
                nextToken();
                if (token.kind == DOT) {
                    JCPrimitiveTypeTree ti = toP(F.at(pos).TypeIdent(TypeTag.VOID));
                    t = bracketsSuffix(ti);
                } else {
                    return illegal(pos);
                }
            } else {
                // Support the corner case of myMethodHandle.<void>invoke() by passing
                // a void type (like other primitive types) to the next phase.
                // The error will be reported in Attr.attribTypes or Attr.visitApply.
                JCPrimitiveTypeTree ti = to(F.at(pos).TypeIdent(TypeTag.VOID));
                //HCZ：nextToken()-？
                nextToken();
                return ti;
                //return illegal();
            }
            break;
        default:
            return illegal();
        }
        return term3Rest(t, typeArgs);
    }
    /**
     * HCZ:?
     */
    JCExpression term3Rest(JCExpression t, List<JCExpression> typeArgs) {
        if (typeArgs != null) illegal();
        while (true) {
            int pos1 = token.pos;
            final List<JCAnnotation> annos = typeAnnotationsOpt();

            if (token.kind == LBRACKET) {
                //HCZ：nextToken()-？
                nextToken();
                if ((mode & TYPE) != 0) {
                    int oldmode = mode;
                    mode = TYPE;
                    if (token.kind == RBRACKET) {
                        //HCZ：nextToken()-？
                        nextToken();
                        t = bracketsOpt(t);
                        t = toP(F.at(pos1).TypeArray(t));
                        if (token.kind == COLCOL) {
                            mode = EXPR;
                            continue;
                        }
                        if (annos.nonEmpty()) {
                            t = toP(F.at(pos1).AnnotatedType(annos, t));
                        }
                        return t;
                    }
                    mode = oldmode;
                }
                if ((mode & EXPR) != 0) {
                    mode = EXPR;
                    JCExpression t1 = term();
                    t = to(F.at(pos1).Indexed(t, t1));
                }
                accept(RBRACKET);
            } else if (token.kind == DOT) {
                //HCZ：nextToken()-？
                nextToken();
                typeArgs = typeArgumentsOpt(EXPR);
                if (token.kind == SUPER && (mode & EXPR) != 0) {
                    mode = EXPR;
                    t = to(F.at(pos1).Select(t, names._super));
                    //HCZ：nextToken()-？
                    nextToken();
                    t = arguments(typeArgs, t);
                    typeArgs = null;
                } else if (token.kind == NEW && (mode & EXPR) != 0) {
                    if (typeArgs != null) return illegal();
                    mode = EXPR;
                    int pos2 = token.pos;
                    //HCZ：nextToken()-？
                    nextToken();
                    if (token.kind == LT) typeArgs = typeArguments(false);
                    t = innerCreator(pos2, typeArgs, t);
                    typeArgs = null;
                } else {
                    List<JCAnnotation> tyannos = null;
                    if ((mode & TYPE) != 0 && token.kind == MONKEYS_AT) {
                        // is the mode check needed?
                        tyannos = typeAnnotationsOpt();
                    }
                    t = toP(F.at(pos1).Select(t, ident()));
                    if (tyannos != null && tyannos.nonEmpty()) {
                        t = toP(F.at(tyannos.head.pos).AnnotatedType(tyannos, t));
                    }
                    t = argumentsOpt(typeArgs, typeArgumentsOpt(t));
                    typeArgs = null;
                }
            } else if ((mode & EXPR) != 0 && token.kind == COLCOL) {
                mode = EXPR;
                if (typeArgs != null) return illegal();
                accept(COLCOL);
                t = memberReferenceSuffix(pos1, t);
            } else {
                if (!annos.isEmpty()) {
                    if (permitTypeAnnotationsPushBack)
                        typeAnnotationsPushedBack = annos;
                    else
                        return illegal(annos.head.pos);
                }
                break;
            }
        }
        while ((token.kind == PLUSPLUS || token.kind == SUBSUB) && (mode & EXPR) != 0) {
            mode = EXPR;
            t = to(F.at(token.pos).Unary(
                  token.kind == PLUSPLUS ? POSTINC : POSTDEC, t));
            //HCZ：nextToken()-？
            nextToken();
        }
        return toP(t);
    }

    /**
     * HCZ:?
     *
     * If we see an identifier followed by a '&lt;' it could be an unbound
     * method reference or a binary expression. To disambiguate, look for a
     * matching '&gt;' and see if the subsequent terminal is either '.' or '::'.
     */
    @SuppressWarnings("fallthrough")
    boolean isUnboundMemberRef() {
        int pos = 0, depth = 0;
        outer: for (Token t = S.token(pos) ; ; t = S.token(++pos)) {
            switch (t.kind) {
                case IDENTIFIER: case UNDERSCORE: case QUES: case EXTENDS: case SUPER:
                case DOT: case RBRACKET: case LBRACKET: case COMMA:
                case BYTE: case SHORT: case INT: case LONG: case FLOAT:
                case DOUBLE: case BOOLEAN: case CHAR:
                case MONKEYS_AT:
                    break;

                case LPAREN:
                    // skip annotation values
                    int nesting = 0;
                    for (; ; pos++) {
                        TokenKind tk2 = S.token(pos).kind;
                        switch (tk2) {
                            case EOF:
                                return false;
                            case LPAREN:
                                nesting++;
                                break;
                            case RPAREN:
                                nesting--;
                                if (nesting == 0) {
                                    continue outer;
                                }
                                break;
                        }
                    }

                case LT:
                    depth++; break;
                case GTGTGT:
                    depth--;
                case GTGT:
                    depth--;
                case GT:
                    depth--;
                    if (depth == 0) {
                        TokenKind nextKind = S.token(pos + 1).kind;
                        return
                            nextKind == TokenKind.DOT ||
                            nextKind == TokenKind.LBRACKET ||
                            nextKind == TokenKind.COLCOL;
                    }
                    break;
                default:
                    return false;
            }
        }
    }

    /**
     * HCZ:?
     *
     * If we see an identifier followed by a '&lt;' it could be an unbound
     * method reference or a binary expression. To disambiguate, look for a
     * matching '&gt;' and see if the subsequent terminal is either '.' or '::'.
     */
    @SuppressWarnings("fallthrough")
    ParensResult analyzeParens() {
        int depth = 0;
        boolean type = false;
        outer: for (int lookahead = 0 ; ; lookahead++) {
            TokenKind tk = S.token(lookahead).kind;
            switch (tk) {
                case COMMA:
                    type = true;
                case EXTENDS: case SUPER: case DOT: case AMP:
                    //skip
                    break;
                case QUES:
                    if (peekToken(lookahead, EXTENDS) ||
                            peekToken(lookahead, SUPER)) {
                        //wildcards
                        type = true;
                    }
                    break;
                case BYTE: case SHORT: case INT: case LONG: case FLOAT:
                case DOUBLE: case BOOLEAN: case CHAR:
                    if (peekToken(lookahead, RPAREN)) {
                        //Type, ')' -> cast
                        return ParensResult.CAST;
                    } else if (peekToken(lookahead, LAX_IDENTIFIER)) {
                        //Type, Identifier/'_'/'assert'/'enum' -> explicit lambda
                        return ParensResult.EXPLICIT_LAMBDA;
                    }
                    break;
                case LPAREN:
                    if (lookahead != 0) {
                        // '(' in a non-starting position -> parens
                        return ParensResult.PARENS;
                    } else if (peekToken(lookahead, RPAREN)) {
                        // '(', ')' -> explicit lambda
                        return ParensResult.EXPLICIT_LAMBDA;
                    }
                    break;
                case RPAREN:
                    // if we have seen something that looks like a type,
                    // then it's a cast expression
                    if (type) return ParensResult.CAST;
                    // otherwise, disambiguate cast vs. parenthesized expression
                    // based on subsequent token.
                    switch (S.token(lookahead + 1).kind) {
                        /*case PLUSPLUS: case SUBSUB: */
                        case BANG: case TILDE:
                        case LPAREN: case THIS: case SUPER:
                        case INTLITERAL: case LONGLITERAL: case FLOATLITERAL:
                        case DOUBLELITERAL: case CHARLITERAL: case STRINGLITERAL:
                        case TRUE: case FALSE: case NULL:
                        case NEW: case IDENTIFIER: case ASSERT: case ENUM: case UNDERSCORE:
                        case BYTE: case SHORT: case CHAR: case INT:
                        case LONG: case FLOAT: case DOUBLE: case BOOLEAN: case VOID:
                            return ParensResult.CAST;
                        default:
                            return ParensResult.PARENS;
                    }
                case UNDERSCORE:
                case ASSERT:
                case ENUM:
                case IDENTIFIER:
                    if (peekToken(lookahead, LAX_IDENTIFIER)) {
                        // Identifier, Identifier/'_'/'assert'/'enum' -> explicit lambda
                        return ParensResult.EXPLICIT_LAMBDA;
                    } else if (peekToken(lookahead, RPAREN, ARROW)) {
                        // Identifier, ')' '->' -> implicit lambda
                        return ParensResult.IMPLICIT_LAMBDA;
                    }
                    type = false;
                    break;
                case FINAL:
                case ELLIPSIS:
                    //those can only appear in explicit lambdas
                    return ParensResult.EXPLICIT_LAMBDA;
                case MONKEYS_AT:
                    type = true;
                    lookahead += 1; //skip '@'
                    while (peekToken(lookahead, DOT)) {
                        lookahead += 2;
                    }
                    if (peekToken(lookahead, LPAREN)) {
                        lookahead++;
                        //skip annotation values
                        int nesting = 0;
                        for (; ; lookahead++) {
                            TokenKind tk2 = S.token(lookahead).kind;
                            switch (tk2) {
                                case EOF:
                                    return ParensResult.PARENS;
                                case LPAREN:
                                    nesting++;
                                    break;
                                case RPAREN:
                                    nesting--;
                                    if (nesting == 0) {
                                        continue outer;
                                    }
                                break;
                            }
                        }
                    }
                    break;
                case LBRACKET:
                    if (peekToken(lookahead, RBRACKET, LAX_IDENTIFIER)) {
                        // '[', ']', Identifier/'_'/'assert'/'enum' -> explicit lambda
                        return ParensResult.EXPLICIT_LAMBDA;
                    } else if (peekToken(lookahead, RBRACKET, RPAREN) ||
                            peekToken(lookahead, RBRACKET, AMP)) {
                        // '[', ']', ')' -> cast
                        // '[', ']', '&' -> cast (intersection type)
                        return ParensResult.CAST;
                    } else if (peekToken(lookahead, RBRACKET)) {
                        //consume the ']' and skip
                        type = true;
                        lookahead++;
                        break;
                    } else {
                        return ParensResult.PARENS;
                    }
                case LT:
                    depth++; break;
                case GTGTGT:
                    depth--;
                case GTGT:
                    depth--;
                case GT:
                    depth--;
                    if (depth == 0) {
                        if (peekToken(lookahead, RPAREN) ||
                                peekToken(lookahead, AMP)) {
                            // '>', ')' -> cast
                            // '>', '&' -> cast
                            return ParensResult.CAST;
                        } else if (peekToken(lookahead, LAX_IDENTIFIER, COMMA) ||
                                peekToken(lookahead, LAX_IDENTIFIER, RPAREN, ARROW) ||
                                peekToken(lookahead, ELLIPSIS)) {
                            // '>', Identifier/'_'/'assert'/'enum', ',' -> explicit lambda
                            // '>', Identifier/'_'/'assert'/'enum', ')', '->' -> explicit lambda
                            // '>', '...' -> explicit lambda
                            return ParensResult.EXPLICIT_LAMBDA;
                        }
                        //it looks a type, but could still be (i) a cast to generic type,
                        //(ii) an unbound method reference or (iii) an explicit lambda
                        type = true;
                        break;
                    } else if (depth < 0) {
                        //unbalanced '<', '>' - not a generic type
                        return ParensResult.PARENS;
                    }
                    break;
                default:
                    //this includes EOF
                    return ParensResult.PARENS;
            }
        }
    }

    /**
     * HCZ:?
     *
     *  Accepts all identifier-like tokens */
    Filter<TokenKind> LAX_IDENTIFIER = new Filter<TokenKind>() {
        public boolean accepts(TokenKind t) {
            return t == IDENTIFIER || t == UNDERSCORE || t == ASSERT || t == ENUM;
        }
    };

    /**
     * HCZ:?
     */
    enum ParensResult {
        CAST,
        EXPLICIT_LAMBDA,
        IMPLICIT_LAMBDA,
        PARENS;
    }

    /**
     * HCZ:?
     */
    JCExpression lambdaExpressionOrStatement(boolean hasParens, boolean explicitParams, int pos) {
        List<JCVariableDecl> params = explicitParams ?
                formalParameters(true) :
                implicitParameters(hasParens);

        return lambdaExpressionOrStatementRest(params, pos);
    }
    /**
     * HCZ:?
     */
    JCExpression lambdaExpressionOrStatementRest(List<JCVariableDecl> args, int pos) {
        checkLambda();
        accept(ARROW);

        return token.kind == LBRACE ?
            lambdaStatement(args, pos, pos) :
            lambdaExpression(args, pos);
    }
    /**
     * HCZ:?
     */
    JCExpression lambdaStatement(List<JCVariableDecl> args, int pos, int pos2) {
        JCBlock block = block(pos2, 0);
        return toP(F.at(pos).Lambda(args, block));
    }
    /**
     * HCZ:?
     */
    JCExpression lambdaExpression(List<JCVariableDecl> args, int pos) {
        JCTree expr = parseExpression();
        return toP(F.at(pos).Lambda(args, expr));
    }

    /**
     * HCZ:?
     *
     *  SuperSuffix = Arguments | "." [TypeArguments] Ident [Arguments]
     */
    JCExpression superSuffix(List<JCExpression> typeArgs, JCExpression t) {
        //HCZ：nextToken()-？
        nextToken();
        if (token.kind == LPAREN || typeArgs != null) {
            t = arguments(typeArgs, t);
        } else if (token.kind == COLCOL) {
            if (typeArgs != null) return illegal();
            t = memberReferenceSuffix(t);
        } else {
            int pos = token.pos;
            accept(DOT);
            typeArgs = (token.kind == LT) ? typeArguments(false) : null;
            t = toP(F.at(pos).Select(t, ident()));
            t = argumentsOpt(typeArgs, t);
        }
        return t;
    }

    /**
     * HCZ:?
     *
     *  BasicType = BYTE | SHORT | CHAR | INT | LONG | FLOAT | DOUBLE | BOOLEAN
     */
    JCPrimitiveTypeTree basicType() {
        JCPrimitiveTypeTree t = to(F.at(token.pos).TypeIdent(typetag(token.kind)));
        //HCZ：nextToken()-？
        nextToken();
        return t;
    }

    /**
     * HCZ:?
     *
     *  ArgumentsOpt = [ Arguments ]
     */
    JCExpression argumentsOpt(List<JCExpression> typeArgs, JCExpression t) {
        if ((mode & EXPR) != 0 && token.kind == LPAREN || typeArgs != null) {
            mode = EXPR;
            return arguments(typeArgs, t);
        } else {
            return t;
        }
    }

    /**
     * HCZ:?
     *  Arguments = "(" [Expression { COMMA Expression }] ")"
     */
    List<JCExpression> arguments() {
        ListBuffer<JCExpression> args = new ListBuffer<>();
        if (token.kind == LPAREN) {
            //HCZ：nextToken()-？
            nextToken();
            if (token.kind != RPAREN) {
                args.append(parseExpression());
                while (token.kind == COMMA) {
                    //HCZ：nextToken()-？
                    nextToken();
                    args.append(parseExpression());
                }
            }
            accept(RPAREN);
        } else {
            syntaxError(token.pos, "expected", LPAREN);
        }
        return args.toList();
    }
    /**
     * HCZ:?
     */
    JCMethodInvocation arguments(List<JCExpression> typeArgs, JCExpression t) {
        int pos = token.pos;
        List<JCExpression> args = arguments();
        return toP(F.at(pos).Apply(typeArgs, t, args));
    }

    /**
     * HCZ:?
     *
     *  TypeArgumentsOpt = [ TypeArguments ]
     */
    JCExpression typeArgumentsOpt(JCExpression t) {
        if (token.kind == LT &&
            (mode & TYPE) != 0 &&
            (mode & NOPARAMS) == 0) {
            mode = TYPE;
            checkGenerics();
            return typeArguments(t, false);
        } else {
            return t;
        }
    }
    /**
     * HCZ:?
     */
    List<JCExpression> typeArgumentsOpt() {
        return typeArgumentsOpt(TYPE);
    }
    /**
     * HCZ:?
     */
    List<JCExpression> typeArgumentsOpt(int useMode) {
        if (token.kind == LT) {
            checkGenerics();
            if ((mode & useMode) == 0 ||
                (mode & NOPARAMS) != 0) {
                illegal();
            }
            mode = useMode;
            return typeArguments(false);
        }
        return null;
    }

    /**
     * HCZ:?
     *
     *  {@literal
     *  TypeArguments  = "<" TypeArgument {"," TypeArgument} ">"
     *  }
     */
    List<JCExpression> typeArguments(boolean diamondAllowed) {
        if (token.kind == LT) {
            //HCZ：nextToken()-？
            nextToken();
            if (token.kind == GT && diamondAllowed) {
                checkDiamond();
                mode |= DIAMOND;
                //HCZ：nextToken()-？
                nextToken();
                return List.nil();
            } else {
                ListBuffer<JCExpression> args = new ListBuffer<>();
                args.append(((mode & EXPR) == 0) ? typeArgument() : parseType());
                while (token.kind == COMMA) {
                    //HCZ：nextToken()-？
                    nextToken();
                    args.append(((mode & EXPR) == 0) ? typeArgument() : parseType());
                }
                switch (token.kind) {

                case GTGTGTEQ: case GTGTEQ: case GTEQ:
                case GTGTGT: case GTGT:
                    token = S.split();
                    break;
                case GT:
                    //HCZ：nextToken()-？
                    nextToken();
                    break;
                default:
                    args.append(syntaxError(token.pos, "expected", GT));
                    break;
                }
                return args.toList();
            }
        } else {
            return List.<JCExpression>of(syntaxError(token.pos, "expected", LT));
        }
    }

    /**
     * HCZ:?
     *
     *  {@literal
     *  TypeArgument = Type
     *               | [Annotations] "?"
     *               | [Annotations] "?" EXTENDS Type {"&" Type}
     *               | [Annotations] "?" SUPER Type
     *  }
     */
    JCExpression typeArgument() {
        List<JCAnnotation> annotations = typeAnnotationsOpt();
        if (token.kind != QUES) return parseType(annotations);
        int pos = token.pos;
        //HCZ：nextToken()-？
        nextToken();
        JCExpression result;
        if (token.kind == EXTENDS) {
            TypeBoundKind t = to(F.at(pos).TypeBoundKind(BoundKind.EXTENDS));
            //HCZ：nextToken()-？
            nextToken();
            JCExpression bound = parseType();
            result = F.at(pos).Wildcard(t, bound);
        } else if (token.kind == SUPER) {
            TypeBoundKind t = to(F.at(pos).TypeBoundKind(BoundKind.SUPER));
            //HCZ：nextToken()-？
            nextToken();
            JCExpression bound = parseType();
            result = F.at(pos).Wildcard(t, bound);
        } else if (LAX_IDENTIFIER.accepts(token.kind)) {
            //error recovery
            TypeBoundKind t = F.at(Position.NOPOS).TypeBoundKind(BoundKind.UNBOUND);
            JCExpression wc = toP(F.at(pos).Wildcard(t, null));
            JCIdent id = toP(F.at(token.pos).Ident(ident()));
            JCErroneous err = F.at(pos).Erroneous(List.<JCTree>of(wc, id));
            reportSyntaxError(err, "expected3", GT, EXTENDS, SUPER);
            result = err;
        } else {
            TypeBoundKind t = toP(F.at(pos).TypeBoundKind(BoundKind.UNBOUND));
            result = toP(F.at(pos).Wildcard(t, null));
        }
        if (!annotations.isEmpty()) {
            result = toP(F.at(annotations.head.pos).AnnotatedType(annotations,result));
        }
        return result;
    }

    /**
     * HCZ:?
     */
    JCTypeApply typeArguments(JCExpression t, boolean diamondAllowed) {
        int pos = token.pos;
        List<JCExpression> args = typeArguments(diamondAllowed);
        return toP(F.at(pos).TypeApply(t, args));
    }

    /**
     * HCZ:?
     *
     * BracketsOpt = { [Annotations] "[" "]" }*
     *
     * <p>
     *
     * <code>annotations</code> is the list of annotations targeting
     * the expression <code>t</code>.
     */
    private JCExpression bracketsOpt(JCExpression t,
            List<JCAnnotation> annotations) {
        List<JCAnnotation> nextLevelAnnotations = typeAnnotationsOpt();

        if (token.kind == LBRACKET) {
            int pos = token.pos;
            //HCZ：nextToken()-？
            nextToken();
            t = bracketsOptCont(t, pos, nextLevelAnnotations);
        } else if (!nextLevelAnnotations.isEmpty()) {
            if (permitTypeAnnotationsPushBack) {
                this.typeAnnotationsPushedBack = nextLevelAnnotations;
            } else {
                return illegal(nextLevelAnnotations.head.pos);
            }
        }

        if (!annotations.isEmpty()) {
            t = toP(F.at(token.pos).AnnotatedType(annotations, t));
        }
        return t;
    }

    /**
     * HCZ:?
     *
     *  BracketsOpt = [ "[" "]" { [Annotations] "[" "]"} ]
     */
    private JCExpression bracketsOpt(JCExpression t) {
        return bracketsOpt(t, List.<JCAnnotation>nil());
    }

    /**
     * HCZ:?
     */
    private JCExpression bracketsOptCont(JCExpression t, int pos,
            List<JCAnnotation> annotations) {
        accept(RBRACKET);
        t = bracketsOpt(t);
        t = toP(F.at(pos).TypeArray(t));
        if (annotations.nonEmpty()) {
            t = toP(F.at(pos).AnnotatedType(annotations, t));
        }
        return t;
    }

    /**
     * HCZ:?
     *
     *  BracketsSuffixExpr = "." CLASS
     *  BracketsSuffixType =
     */
    JCExpression bracketsSuffix(JCExpression t) {
        if ((mode & EXPR) != 0 && token.kind == DOT) {
            mode = EXPR;
            int pos = token.pos;
            //HCZ：nextToken()-？
            nextToken();
            accept(CLASS);
            if (token.pos == endPosTable.errorEndPos) {
                // error recovery
                Name name;
                if (LAX_IDENTIFIER.accepts(token.kind)) {
                    name = token.name();
                    //HCZ：nextToken()-？
                    nextToken();
                } else {
                    name = names.error;
                }
                t = F.at(pos).Erroneous(List.<JCTree>of(toP(F.at(pos).Select(t, name))));
            } else {
                t = toP(F.at(pos).Select(t, names._class));
            }
        } else if ((mode & TYPE) != 0) {
            if (token.kind != COLCOL) {
                mode = TYPE;
            }
        } else if (token.kind != COLCOL) {
            syntaxError(token.pos, "dot.class.expected");
        }
        return t;
    }

    /**
     * HCZ:?
     *
     * MemberReferenceSuffix = "::" [TypeArguments] Ident
     *                       | "::" [TypeArguments] "new"
     */
    JCExpression memberReferenceSuffix(JCExpression t) {
        int pos1 = token.pos;
        accept(COLCOL);
        return memberReferenceSuffix(pos1, t);
    }

    /**
     * HCZ:?
     */
    JCExpression memberReferenceSuffix(int pos1, JCExpression t) {
        checkMethodReferences();
        mode = EXPR;
        List<JCExpression> typeArgs = null;
        if (token.kind == LT) {
            typeArgs = typeArguments(false);
        }
        Name refName;
        ReferenceMode refMode;
        if (token.kind == NEW) {
            refMode = ReferenceMode.NEW;
            refName = names.init;
            //HCZ：nextToken()-？
            nextToken();
        } else {
            refMode = ReferenceMode.INVOKE;
            refName = ident();
        }
        return toP(F.at(t.getStartPosition()).Reference(refMode, refName, t, typeArgs));
    }

    /**
     * HCZ:?
     *
     *  Creator = [Annotations] Qualident [TypeArguments] ( ArrayCreatorRest | ClassCreatorRest )
     */
    JCExpression creator(int newpos, List<JCExpression> typeArgs) {
        List<JCAnnotation> newAnnotations = annotationsOpt(Tag.ANNOTATION);

        switch (token.kind) {
        case BYTE: case SHORT: case CHAR: case INT: case LONG: case FLOAT:
        case DOUBLE: case BOOLEAN:
            if (typeArgs == null) {
                if (newAnnotations.isEmpty()) {
                    return arrayCreatorRest(newpos, basicType());
                } else {
                    return arrayCreatorRest(newpos, toP(F.at(newAnnotations.head.pos).AnnotatedType(newAnnotations, basicType())));
                }
            }
            break;
        default:
        }
        JCExpression t = qualident(true);

        int oldmode = mode;
        mode = TYPE;
        boolean diamondFound = false;
        int lastTypeargsPos = -1;
        if (token.kind == LT) {
            checkGenerics();
            lastTypeargsPos = token.pos;
            t = typeArguments(t, true);
            diamondFound = (mode & DIAMOND) != 0;
        }
        while (token.kind == DOT) {
            if (diamondFound) {
                //cannot select after a diamond
                illegal();
            }
            int pos = token.pos;
            //HCZ：nextToken()-？
            nextToken();
            List<JCAnnotation> tyannos = typeAnnotationsOpt();
            t = toP(F.at(pos).Select(t, ident()));

            if (tyannos != null && tyannos.nonEmpty()) {
                t = toP(F.at(tyannos.head.pos).AnnotatedType(tyannos, t));
            }

            if (token.kind == LT) {
                lastTypeargsPos = token.pos;
                checkGenerics();
                t = typeArguments(t, true);
                diamondFound = (mode & DIAMOND) != 0;
            }
        }
        mode = oldmode;
        if (token.kind == LBRACKET || token.kind == MONKEYS_AT) {
            // handle type annotations for non primitive arrays
            if (newAnnotations.nonEmpty()) {
                t = insertAnnotationsToMostInner(t, newAnnotations, false);
            }

            JCExpression e = arrayCreatorRest(newpos, t);
            if (diamondFound) {
                reportSyntaxError(lastTypeargsPos, "cannot.create.array.with.diamond");
                return toP(F.at(newpos).Erroneous(List.of(e)));
            }
            else if (typeArgs != null) {
                int pos = newpos;
                if (!typeArgs.isEmpty() && typeArgs.head.pos != Position.NOPOS) {
                    // note: this should always happen but we should
                    // not rely on this as the parser is continuously
                    // modified to improve error recovery.
                    pos = typeArgs.head.pos;
                }
                setErrorEndPos(S.prevToken().endPos);
                JCErroneous err = F.at(pos).Erroneous(typeArgs.prepend(e));
                reportSyntaxError(err, "cannot.create.array.with.type.arguments");
                return toP(err);
            }
            return e;
        } else if (token.kind == LPAREN) {
            JCNewClass newClass = classCreatorRest(newpos, null, typeArgs, t);
            if (newClass.def != null) {
                assert newClass.def.mods.annotations.isEmpty();
                if (newAnnotations.nonEmpty()) {
                    // Add type and declaration annotations to the new class;
                    // com.sun.tools.javac.code.TypeAnnotations.TypeAnnotationPositions.visitNewClass(JCNewClass)
                    // will later remove all type annotations and only leave the
                    // declaration annotations.
                    newClass.def.mods.pos = earlier(newClass.def.mods.pos, newAnnotations.head.pos);
                    newClass.def.mods.annotations = newAnnotations;
                }
            } else {
                // handle type annotations for instantiations
                if (newAnnotations.nonEmpty()) {
                    t = insertAnnotationsToMostInner(t, newAnnotations, false);
                    newClass.clazz = t;
                }
            }
            return newClass;
        } else {
            setErrorEndPos(token.pos);
            reportSyntaxError(token.pos, "expected2", LPAREN, LBRACKET);
            t = toP(F.at(newpos).NewClass(null, typeArgs, t, List.<JCExpression>nil(), null));
            return toP(F.at(newpos).Erroneous(List.<JCTree>of(t)));
        }
    }

    /**
     * HCZ:?
     *
     *  InnerCreator = [Annotations] Ident [TypeArguments] ClassCreatorRest
     */
    JCExpression innerCreator(int newpos, List<JCExpression> typeArgs, JCExpression encl) {
        List<JCAnnotation> newAnnotations = typeAnnotationsOpt();

        JCExpression t = toP(F.at(token.pos).Ident(ident()));

        if (newAnnotations.nonEmpty()) {
            t = toP(F.at(newAnnotations.head.pos).AnnotatedType(newAnnotations, t));
        }

        if (token.kind == LT) {
            int oldmode = mode;
            checkGenerics();
            t = typeArguments(t, true);
            mode = oldmode;
        }
        return classCreatorRest(newpos, encl, typeArgs, t);
    }

    /**
     * HCZ:?
     *
     *  ArrayCreatorRest = [Annotations] "[" ( "]" BracketsOpt ArrayInitializer
     *                         | Expression "]" {[Annotations]  "[" Expression "]"} BracketsOpt )
     */
    JCExpression arrayCreatorRest(int newpos, JCExpression elemtype) {
        List<JCAnnotation> annos = typeAnnotationsOpt();

        accept(LBRACKET);
        if (token.kind == RBRACKET) {
            accept(RBRACKET);
            elemtype = bracketsOpt(elemtype, annos);
            if (token.kind == LBRACE) {
                JCNewArray na = (JCNewArray)arrayInitializer(newpos, elemtype);
                if (annos.nonEmpty()) {
                    // when an array initializer is present then
                    // the parsed annotations should target the
                    // new array tree
                    // bracketsOpt inserts the annotation in
                    // elemtype, and it needs to be corrected
                    //
                    JCAnnotatedType annotated = (JCAnnotatedType)elemtype;
                    assert annotated.annotations == annos;
                    na.annotations = annotated.annotations;
                    na.elemtype = annotated.underlyingType;
                }
                return na;
            } else {
                JCExpression t = toP(F.at(newpos).NewArray(elemtype, List.<JCExpression>nil(), null));
                return syntaxError(token.pos, List.<JCTree>of(t), "array.dimension.missing");
            }
        } else {
            ListBuffer<JCExpression> dims = new ListBuffer<JCExpression>();

            // maintain array dimension type annotations
            ListBuffer<List<JCAnnotation>> dimAnnotations = new ListBuffer<>();
            dimAnnotations.append(annos);

            dims.append(parseExpression());
            accept(RBRACKET);
            while (token.kind == LBRACKET
                    || token.kind == MONKEYS_AT) {
                List<JCAnnotation> maybeDimAnnos = typeAnnotationsOpt();
                int pos = token.pos;
                //HCZ：nextToken()-？
                nextToken();
                if (token.kind == RBRACKET) {
                    elemtype = bracketsOptCont(elemtype, pos, maybeDimAnnos);
                } else {
                    if (token.kind == RBRACKET) { // no dimension
                        elemtype = bracketsOptCont(elemtype, pos, maybeDimAnnos);
                    } else {
                        dimAnnotations.append(maybeDimAnnos);
                        dims.append(parseExpression());
                        accept(RBRACKET);
                    }
                }
            }

            JCNewArray na = toP(F.at(newpos).NewArray(elemtype, dims.toList(), null));
            na.dimAnnotations = dimAnnotations.toList();
            return na;
        }
    }

    /**
     * HCZ:?
     *
     *  ClassCreatorRest = Arguments [ClassBody]
     */
    JCNewClass classCreatorRest(int newpos,
                                  JCExpression encl,
                                  List<JCExpression> typeArgs,
                                  JCExpression t)
    {
        List<JCExpression> args = arguments();
        JCClassDecl body = null;
        if (token.kind == LBRACE) {
            int pos = token.pos;
            List<JCTree> defs = classOrInterfaceBody(names.empty, false);
            JCModifiers mods = F.at(Position.NOPOS).Modifiers(0);
            body = toP(F.at(pos).AnonymousClassDef(mods, defs));
        }
        return toP(F.at(newpos).NewClass(encl, typeArgs, t, args, body));
    }

    /**
     * HCZ:?
     *
     *  ArrayInitializer = "{" [VariableInitializer {"," VariableInitializer}] [","] "}"
     */
    JCExpression arrayInitializer(int newpos, JCExpression t) {
        accept(LBRACE);
        ListBuffer<JCExpression> elems = new ListBuffer<JCExpression>();
        if (token.kind == COMMA) {
            //HCZ：nextToken()-？
            nextToken();
        } else if (token.kind != RBRACE) {
            elems.append(variableInitializer());
            while (token.kind == COMMA) {
                //HCZ：nextToken()-？
                nextToken();
                if (token.kind == RBRACE) break;
                elems.append(variableInitializer());
            }
        }
        accept(RBRACE);
        return toP(F.at(newpos).NewArray(t, List.<JCExpression>nil(), elems.toList()));
    }

    /**
     * HCZ:?
     *
     *  VariableInitializer = ArrayInitializer | Expression
     */
    public JCExpression variableInitializer() {
        return token.kind == LBRACE ? arrayInitializer(token.pos, null) : parseExpression();
    }

    /**
     * HCZ:?
     *
     *  ParExpression = "(" Expression ")"
     */
    JCExpression parExpression() {
        int pos = token.pos;
        accept(LPAREN);
        JCExpression t = parseExpression();
        accept(RPAREN);
        return toP(F.at(pos).Parens(t));
    }

    /**
     * HCZ:?
     *
     * Block = "{" BlockStatements "}"
     */
    JCBlock block(int pos, long flags) {
        accept(LBRACE);
        List<JCStatement> stats = blockStatements();
        JCBlock t = F.at(pos).Block(flags, stats);
        while (token.kind == CASE || token.kind == DEFAULT) {
            syntaxError("orphaned", token.kind);
            switchBlockStatementGroups();
        }
        // the Block node has a field "endpos" for first char of last token, which is
        // usually but not necessarily the last char of the last token.
        t.endpos = token.pos;
        accept(RBRACE);
        return toP(t);
    }

    /**
     * HCZ:?
     */
    public JCBlock block() {
        return block(token.pos, 0);
    }

    /**
     * HCZ:?
     *
     *  BlockStatements = { BlockStatement }
     *  BlockStatement  = LocalVariableDeclarationStatement
     *                  | ClassOrInterfaceOrEnumDeclaration
     *                  | [Ident ":"] Statement
     *  LocalVariableDeclarationStatement
     *                  = { FINAL | '@' Annotation } Type VariableDeclarators ";"
     */
    @SuppressWarnings("fallthrough")
    List<JCStatement> blockStatements() {
        //todo: skip to anchor on error(?)
        ListBuffer<JCStatement> stats = new ListBuffer<JCStatement>();
        while (true) {
            List<JCStatement> stat = blockStatement();
            if (stat.isEmpty()) {
                return stats.toList();
            } else {
                if (token.pos <= endPosTable.errorEndPos) {
                    skip(false, true, true, true);
                }
                stats.addAll(stat);
            }
        }
    }

    /**
     * HCZ:?
     *
     * This method parses a statement treating it as a block, relaxing the
     * JLS restrictions, allows us to parse more faulty code, doing so
     * enables us to provide better and accurate diagnostics to the user.
     */
    JCStatement parseStatementAsBlock() {
        int pos = token.pos;
        List<JCStatement> stats = blockStatement();
        if (stats.isEmpty()) {
            JCErroneous e = F.at(pos).Erroneous();
            error(e, "illegal.start.of.stmt");
            return F.at(pos).Exec(e);
        } else {
            JCStatement first = stats.head;
            String error = null;
            switch (first.getTag()) {
            case CLASSDEF:
                error = "class.not.allowed";
                break;
            case VARDEF:
                error = "variable.not.allowed";
                break;
            }
            if (error != null) {
                error(first, error);
                List<JCBlock> blist = List.of(F.at(first.pos).Block(0, stats));
                return toP(F.at(pos).Exec(F.at(first.pos).Erroneous(blist)));
            }
            return first;
        }
    }

    /**
     * HCZ:?
     */
    @SuppressWarnings("fallthrough")
    List<JCStatement> blockStatement() {
        //todo: skip to anchor on error(?)
        int pos = token.pos;
        switch (token.kind) {
        case RBRACE: case CASE: case DEFAULT: case EOF:
            return List.nil();
        case LBRACE: case IF: case FOR: case WHILE: case DO: case TRY:
        case SWITCH: case SYNCHRONIZED: case RETURN: case THROW: case BREAK:
        case CONTINUE: case SEMI: case ELSE: case FINALLY: case CATCH:
            return List.of(parseStatement());
        case MONKEYS_AT:
        case FINAL: {
            Comment dc = token.comment(CommentStyle.JAVADOC);
            JCModifiers mods = modifiersOpt();
            if (token.kind == INTERFACE ||
                token.kind == CLASS ||
                allowEnums && token.kind == ENUM) {
                return List.of(classOrInterfaceOrEnumDeclaration(mods, dc));
            } else {
                JCExpression t = parseType();
                ListBuffer<JCStatement> stats =
                        variableDeclarators(mods, t, new ListBuffer<JCStatement>());
                // A "LocalVariableDeclarationStatement" subsumes the terminating semicolon
                storeEnd(stats.last(), token.endPos);
                accept(SEMI);
                return stats.toList();
            }
        }
        case ABSTRACT: case STRICTFP: {
            Comment dc = token.comment(CommentStyle.JAVADOC);
            JCModifiers mods = modifiersOpt();
            return List.of(classOrInterfaceOrEnumDeclaration(mods, dc));
        }
        case INTERFACE:
        case CLASS:
            Comment dc = token.comment(CommentStyle.JAVADOC);
            return List.of(classOrInterfaceOrEnumDeclaration(modifiersOpt(), dc));
        case ENUM:
        case ASSERT:
            if (allowEnums && token.kind == ENUM) {
                error(token.pos, "local.enum");
                dc = token.comment(CommentStyle.JAVADOC);
                return List.of(classOrInterfaceOrEnumDeclaration(modifiersOpt(), dc));
            } else if (allowAsserts && token.kind == ASSERT) {
                return List.of(parseStatement());
            }
            /* fall through to default */
        default:
            Token prevToken = token;
            JCExpression t = term(EXPR | TYPE);
            if (token.kind == COLON && t.hasTag(IDENT)) {
                //HCZ：nextToken()-？
                nextToken();
                JCStatement stat = parseStatement();
                return List.<JCStatement>of(F.at(pos).Labelled(prevToken.name(), stat));
            } else if ((lastmode & TYPE) != 0 && LAX_IDENTIFIER.accepts(token.kind)) {
                pos = token.pos;
                JCModifiers mods = F.at(Position.NOPOS).Modifiers(0);
                F.at(pos);
                ListBuffer<JCStatement> stats =
                        variableDeclarators(mods, t, new ListBuffer<JCStatement>());
                // A "LocalVariableDeclarationStatement" subsumes the terminating semicolon
                storeEnd(stats.last(), token.endPos);
                accept(SEMI);
                return stats.toList();
            } else {
                // This Exec is an "ExpressionStatement"; it subsumes the terminating semicolon
                JCExpressionStatement expr = to(F.at(pos).Exec(checkExprStat(t)));
                accept(SEMI);
                return List.<JCStatement>of(expr);
            }
        }
    }

    /**
     * HCZ:?
     *
     *  Statement =
     *       Block
     *     | IF ParExpression Statement [ELSE Statement]
     *     | FOR "(" ForInitOpt ";" [Expression] ";" ForUpdateOpt ")" Statement
     *     | FOR "(" FormalParameter : Expression ")" Statement
     *     | WHILE ParExpression Statement
     *     | DO Statement WHILE ParExpression ";"
     *     | TRY Block ( Catches | [Catches] FinallyPart )
     *     | TRY "(" ResourceSpecification ";"opt ")" Block [Catches] [FinallyPart]
     *     | SWITCH ParExpression "{" SwitchBlockStatementGroups "}"
     *     | SYNCHRONIZED ParExpression Block
     *     | RETURN [Expression] ";"
     *     | THROW Expression ";"
     *     | BREAK [Ident] ";"
     *     | CONTINUE [Ident] ";"
     *     | ASSERT Expression [ ":" Expression ] ";"
     *     | ";"
     *     | ExpressionStatement
     *     | Ident ":" Statement
     */
    @SuppressWarnings("fallthrough")
    public JCStatement parseStatement() {
        int pos = token.pos;
        switch (token.kind) {
        case LBRACE:
            return block();
        case IF: {
            //HCZ：nextToken()-？
            nextToken();
            JCExpression cond = parExpression();
            JCStatement thenpart = parseStatementAsBlock();
            JCStatement elsepart = null;
            if (token.kind == ELSE) {
                //HCZ：nextToken()-？
                nextToken();
                elsepart = parseStatementAsBlock();
            }
            return F.at(pos).If(cond, thenpart, elsepart);
        }
        case FOR: {
            //HCZ：nextToken()-？
            nextToken();
            accept(LPAREN);
            List<JCStatement> inits = token.kind == SEMI ? List.<JCStatement>nil() : forInit();
            if (inits.length() == 1 &&
                inits.head.hasTag(VARDEF) &&
                ((JCVariableDecl) inits.head).init == null &&
                token.kind == COLON) {
                checkForeach();
                JCVariableDecl var = (JCVariableDecl)inits.head;
                accept(COLON);
                JCExpression expr = parseExpression();
                accept(RPAREN);
                JCStatement body = parseStatementAsBlock();
                return F.at(pos).ForeachLoop(var, expr, body);
            } else {
                accept(SEMI);
                JCExpression cond = token.kind == SEMI ? null : parseExpression();
                accept(SEMI);
                List<JCExpressionStatement> steps = token.kind == RPAREN ? List.<JCExpressionStatement>nil() : forUpdate();
                accept(RPAREN);
                JCStatement body = parseStatementAsBlock();
                return F.at(pos).ForLoop(inits, cond, steps, body);
            }
        }
        case WHILE: {
            //HCZ：nextToken()-？
            nextToken();
            JCExpression cond = parExpression();
            JCStatement body = parseStatementAsBlock();
            return F.at(pos).WhileLoop(cond, body);
        }
        case DO: {
            //HCZ：nextToken()-？
            nextToken();
            JCStatement body = parseStatementAsBlock();
            accept(WHILE);
            JCExpression cond = parExpression();
            JCDoWhileLoop t = to(F.at(pos).DoLoop(body, cond));
            accept(SEMI);
            return t;
        }
        case TRY: {
            //HCZ：nextToken()-？
            nextToken();
            List<JCTree> resources = List.<JCTree>nil();
            if (token.kind == LPAREN) {
                checkTryWithResources();
                //HCZ：nextToken()-？
                nextToken();
                resources = resources();
                accept(RPAREN);
            }
            JCBlock body = block();
            ListBuffer<JCCatch> catchers = new ListBuffer<JCCatch>();
            JCBlock finalizer = null;
            if (token.kind == CATCH || token.kind == FINALLY) {
                while (token.kind == CATCH) catchers.append(catchClause());
                if (token.kind == FINALLY) {
                    //HCZ：nextToken()-？
                    nextToken();
                    finalizer = block();
                }
            } else {
                if (allowTWR) {
                    if (resources.isEmpty())
                        error(pos, "try.without.catch.finally.or.resource.decls");
                } else
                    error(pos, "try.without.catch.or.finally");
            }
            return F.at(pos).Try(resources, body, catchers.toList(), finalizer);
        }
        case SWITCH: {
            //HCZ：nextToken()-？
            nextToken();
            JCExpression selector = parExpression();
            accept(LBRACE);
            List<JCCase> cases = switchBlockStatementGroups();
            JCSwitch t = to(F.at(pos).Switch(selector, cases));
            accept(RBRACE);
            return t;
        }
        case SYNCHRONIZED: {
            //HCZ：nextToken()-？
            nextToken();
            JCExpression lock = parExpression();
            JCBlock body = block();
            return F.at(pos).Synchronized(lock, body);
        }
        case RETURN: {
            //HCZ：nextToken()-？
            nextToken();
            JCExpression result = token.kind == SEMI ? null : parseExpression();
            JCReturn t = to(F.at(pos).Return(result));
            accept(SEMI);
            return t;
        }
        case THROW: {
            //HCZ：nextToken()-？
            nextToken();
            JCExpression exc = parseExpression();
            JCThrow t = to(F.at(pos).Throw(exc));
            accept(SEMI);
            return t;
        }
        case BREAK: {
            //HCZ：nextToken()-？
            nextToken();
            Name label = LAX_IDENTIFIER.accepts(token.kind) ? ident() : null;
            JCBreak t = to(F.at(pos).Break(label));
            accept(SEMI);
            return t;
        }
        case CONTINUE: {
            //HCZ：nextToken()-？
            nextToken();
            Name label = LAX_IDENTIFIER.accepts(token.kind) ? ident() : null;
            JCContinue t =  to(F.at(pos).Continue(label));
            accept(SEMI);
            return t;
        }
        case SEMI:
            //HCZ：nextToken()-？
            nextToken();
            return toP(F.at(pos).Skip());
        case ELSE:
            int elsePos = token.pos;
            //HCZ：nextToken()-？
            nextToken();
            return doRecover(elsePos, BasicErrorRecoveryAction.BLOCK_STMT, "else.without.if");
        case FINALLY:
            int finallyPos = token.pos;
            //HCZ：nextToken()-？
            nextToken();
            return doRecover(finallyPos, BasicErrorRecoveryAction.BLOCK_STMT, "finally.without.try");
        case CATCH:
            return doRecover(token.pos, BasicErrorRecoveryAction.CATCH_CLAUSE, "catch.without.try");
        case ASSERT: {
            if (allowAsserts && token.kind == ASSERT) {
                //HCZ：nextToken()-？
                nextToken();
                JCExpression assertion = parseExpression();
                JCExpression message = null;
                if (token.kind == COLON) {
                    //HCZ：nextToken()-？
                    nextToken();
                    message = parseExpression();
                }
                JCAssert t = to(F.at(pos).Assert(assertion, message));
                accept(SEMI);
                return t;
            }
            /* else fall through to default case */
        }
        case ENUM:
        default:
            Token prevToken = token;
            JCExpression expr = parseExpression();
            if (token.kind == COLON && expr.hasTag(IDENT)) {
                //HCZ：nextToken()-？
                nextToken();
                JCStatement stat = parseStatement();
                return F.at(pos).Labelled(prevToken.name(), stat);
            } else {
                // This Exec is an "ExpressionStatement"; it subsumes the terminating semicolon
                JCExpressionStatement stat = to(F.at(pos).Exec(checkExprStat(expr)));
                accept(SEMI);
                return stat;
            }
        }
    }

    /**
     * HCZ:?
     */
    private JCStatement doRecover(int startPos, ErrorRecoveryAction action, String key) {
        int errPos = S.errPos();
        JCTree stm = action.doRecover(this);
        S.errPos(errPos);
        return toP(F.Exec(syntaxError(startPos, List.<JCTree>of(stm), key)));
    }

    /**
     * HCZ:?
     *
     *  CatchClause     = CATCH "(" FormalParameter ")" Block
     * TODO: the "FormalParameter" is not correct, it uses the special "catchTypes" rule below.
     */
    protected JCCatch catchClause() {
        int pos = token.pos;
        accept(CATCH);
        accept(LPAREN);
        JCModifiers mods = optFinal(Flags.PARAMETER);
        List<JCExpression> catchTypes = catchTypes();
        JCExpression paramType = catchTypes.size() > 1 ?
                toP(F.at(catchTypes.head.getStartPosition()).TypeUnion(catchTypes)) :
                catchTypes.head;
        JCVariableDecl formal = variableDeclaratorId(mods, paramType);
        accept(RPAREN);
        JCBlock body = block();
        return F.at(pos).Catch(formal, body);
    }

    /**
     * HCZ:?
     */
    List<JCExpression> catchTypes() {
        ListBuffer<JCExpression> catchTypes = new ListBuffer<>();
        catchTypes.add(parseType());
        while (token.kind == BAR) {
            checkMulticatch();
            //HCZ：nextToken()-？
            nextToken();
            // Instead of qualident this is now parseType.
            // But would that allow too much, e.g. arrays or generics?
            catchTypes.add(parseType());
        }
        return catchTypes.toList();
    }

    /**
     * HCZ:?
     *
     *  SwitchBlockStatementGroups = { SwitchBlockStatementGroup }
     *  SwitchBlockStatementGroup = SwitchLabel BlockStatements
     *  SwitchLabel = CASE ConstantExpression ":" | DEFAULT ":"
     */
    List<JCCase> switchBlockStatementGroups() {
        ListBuffer<JCCase> cases = new ListBuffer<JCCase>();
        while (true) {
            int pos = token.pos;
            switch (token.kind) {
            case CASE:
            case DEFAULT:
                cases.append(switchBlockStatementGroup());
                break;
            case RBRACE: case EOF:
                return cases.toList();
            default:
                //HCZ：nextToken()-？
                nextToken(); // to ensure progress
                syntaxError(pos, "expected3",
                    CASE, DEFAULT, RBRACE);
            }
        }
    }

    /**
     * HCZ:?
     */
    protected JCCase switchBlockStatementGroup() {
        int pos = token.pos;
        List<JCStatement> stats;
        JCCase c;
        switch (token.kind) {
        case CASE:
            //HCZ：nextToken()-？
            nextToken();
            JCExpression pat = parseExpression();
            accept(COLON);
            stats = blockStatements();
            c = F.at(pos).Case(pat, stats);
            if (stats.isEmpty())
                storeEnd(c, S.prevToken().endPos);
            return c;
        case DEFAULT:
            //HCZ：nextToken()-？
            nextToken();
            accept(COLON);
            stats = blockStatements();
            c = F.at(pos).Case(null, stats);
            if (stats.isEmpty())
                storeEnd(c, S.prevToken().endPos);
            return c;
        }
        throw new AssertionError("should not reach here");
    }

    /**
     * HCZ:?
     *
     *  MoreStatementExpressions = { COMMA StatementExpression }
     */
    <T extends ListBuffer<? super JCExpressionStatement>> T moreStatementExpressions(int pos,
                                                                    JCExpression first,
                                                                    T stats) {
        // This Exec is a "StatementExpression"; it subsumes no terminating token
        stats.append(toP(F.at(pos).Exec(checkExprStat(first))));
        while (token.kind == COMMA) {
            //HCZ：nextToken()-？
            nextToken();
            pos = token.pos;
            JCExpression t = parseExpression();
            // This Exec is a "StatementExpression"; it subsumes no terminating token
            stats.append(toP(F.at(pos).Exec(checkExprStat(t))));
        }
        return stats;
    }

    /**
     * HCZ:?
     *
     *  ForInit = StatementExpression MoreStatementExpressions
     *           |  { FINAL | '@' Annotation } Type VariableDeclarators
     */
    List<JCStatement> forInit() {
        ListBuffer<JCStatement> stats = new ListBuffer<>();
        int pos = token.pos;
        if (token.kind == FINAL || token.kind == MONKEYS_AT) {
            return variableDeclarators(optFinal(0), parseType(), stats).toList();
        } else {
            JCExpression t = term(EXPR | TYPE);
            if ((lastmode & TYPE) != 0 && LAX_IDENTIFIER.accepts(token.kind)) {
                return variableDeclarators(modifiersOpt(), t, stats).toList();
            } else if ((lastmode & TYPE) != 0 && token.kind == COLON) {
                error(pos, "bad.initializer", "for-loop");
                return List.of((JCStatement)F.at(pos).VarDef(null, null, t, null));
            } else {
                return moreStatementExpressions(pos, t, stats).toList();
            }
        }
    }

    /**
     * HCZ:?
     *
     *  ForUpdate = StatementExpression MoreStatementExpressions
     */
    List<JCExpressionStatement> forUpdate() {
        return moreStatementExpressions(token.pos,
                                        parseExpression(),
                                        new ListBuffer<JCExpressionStatement>()).toList();
    }

    /**
     * HCZ:?
     *
     *  AnnotationsOpt = { '@' Annotation }
     *
     * @param kind Whether to parse an ANNOTATION or TYPE_ANNOTATION
     */
    List<JCAnnotation> annotationsOpt(Tag kind) {
        if (token.kind != MONKEYS_AT) return List.nil(); // optimization
        ListBuffer<JCAnnotation> buf = new ListBuffer<JCAnnotation>();
        int prevmode = mode;
        while (token.kind == MONKEYS_AT) {
            int pos = token.pos;
            //HCZ：nextToken()-？
            nextToken();
            buf.append(annotation(pos, kind));
        }
        lastmode = mode;
        mode = prevmode;
        List<JCAnnotation> annotations = buf.toList();

        return annotations;
    }
    /**
     * HCZ:?
     */
    List<JCAnnotation> typeAnnotationsOpt() {
        List<JCAnnotation> annotations = annotationsOpt(Tag.TYPE_ANNOTATION);
        return annotations;
    }

    /** ModifiersOpt = { Modifier }
     *  Modifier = PUBLIC | PROTECTED | PRIVATE | STATIC | ABSTRACT | FINAL
     *           | NATIVE | SYNCHRONIZED | TRANSIENT | VOLATILE | "@"
     *           | "@" Annotation
     */
    JCModifiers modifiersOpt() {
        return modifiersOpt(null);
    }
    protected JCModifiers modifiersOpt(JCModifiers partial) {
        long flags;
        ListBuffer<JCAnnotation> annotations = new ListBuffer<JCAnnotation>();
        int pos;
        if (partial == null) {
            flags = 0;
            pos = token.pos;
        } else {
            flags = partial.flags;
            annotations.appendList(partial.annotations);
            pos = partial.pos;
        }
        if (token.deprecatedFlag()) {
            flags |= Flags.DEPRECATED;
        }
        int lastPos;
    loop:
        while (true) {
            long flag;
            switch (token.kind) {
            case PRIVATE     : flag = Flags.PRIVATE; break;
            case PROTECTED   : flag = Flags.PROTECTED; break;
            case PUBLIC      : flag = Flags.PUBLIC; break;
            case STATIC      : flag = Flags.STATIC; break;
            case TRANSIENT   : flag = Flags.TRANSIENT; break;
            case FINAL       : flag = Flags.FINAL; break;
            case ABSTRACT    : flag = Flags.ABSTRACT; break;
            case NATIVE      : flag = Flags.NATIVE; break;
            case VOLATILE    : flag = Flags.VOLATILE; break;
            case SYNCHRONIZED: flag = Flags.SYNCHRONIZED; break;
            case STRICTFP    : flag = Flags.STRICTFP; break;
            case MONKEYS_AT  : flag = Flags.ANNOTATION; break;
            case DEFAULT     : checkDefaultMethods(); flag = Flags.DEFAULT; break;
            case ERROR       : flag = 0; nextToken(); break;//HCZ：nextToken()-？
            default: break loop;
            }
            if ((flags & flag) != 0) error(token.pos, "repeated.modifier");
            lastPos = token.pos;
            //HCZ：nextToken()-？
            nextToken();
            if (flag == Flags.ANNOTATION) {
                checkAnnotations();
                if (token.kind != INTERFACE) {
                    JCAnnotation ann = annotation(lastPos, Tag.ANNOTATION);
                    // if first modifier is an annotation, set pos to annotation's.
                    if (flags == 0 && annotations.isEmpty())
                        pos = ann.pos;
                    annotations.append(ann);
                    flag = 0;
                }
            }
            flags |= flag;
        }
        switch (token.kind) {
        case ENUM: flags |= Flags.ENUM; break;
        case INTERFACE: flags |= Flags.INTERFACE; break;
        default: break;
        }

        /* A modifiers tree with no modifier tokens or annotations
         * has no text position. */
        if ((flags & (Flags.ModifierFlags | Flags.ANNOTATION)) == 0 && annotations.isEmpty())
            pos = Position.NOPOS;

        JCModifiers mods = F.at(pos).Modifiers(flags, annotations.toList());
        if (pos != Position.NOPOS)
            storeEnd(mods, S.prevToken().endPos);
        return mods;
    }

    /**
     * HCZ:?
     *
     *  Annotation              = "@" Qualident [ "(" AnnotationFieldValues ")" ]
     *
     * @param pos position of "@" token
     * @param kind Whether to parse an ANNOTATION or TYPE_ANNOTATION
     */
    JCAnnotation annotation(int pos, Tag kind) {
        // accept(AT); // AT consumed by caller
        checkAnnotations();
        if (kind == Tag.TYPE_ANNOTATION) {
            checkTypeAnnotations();
        }
        JCTree ident = qualident(false);
        List<JCExpression> fieldValues = annotationFieldValuesOpt();
        JCAnnotation ann;
        if (kind == Tag.ANNOTATION) {
            ann = F.at(pos).Annotation(ident, fieldValues);
        } else if (kind == Tag.TYPE_ANNOTATION) {
            ann = F.at(pos).TypeAnnotation(ident, fieldValues);
        } else {
            throw new AssertionError("Unhandled annotation kind: " + kind);
        }

        storeEnd(ann, S.prevToken().endPos);
        return ann;
    }
    /**
     * HCZ:?
     */
    List<JCExpression> annotationFieldValuesOpt() {
        return (token.kind == LPAREN) ? annotationFieldValues() : List.<JCExpression>nil();
    }

    /**
     * HCZ:?
     *
     *  AnnotationFieldValues   = "(" [ AnnotationFieldValue { "," AnnotationFieldValue } ] ")" */
    List<JCExpression> annotationFieldValues() {
        accept(LPAREN);
        ListBuffer<JCExpression> buf = new ListBuffer<JCExpression>();
        if (token.kind != RPAREN) {
            buf.append(annotationFieldValue());
            while (token.kind == COMMA) {
                //HCZ：nextToken()-？
                nextToken();
                buf.append(annotationFieldValue());
            }
        }
        accept(RPAREN);
        return buf.toList();
    }

    /**
     * HCZ:?
     *
     *  AnnotationFieldValue    = AnnotationValue
     *                          | Identifier "=" AnnotationValue
     */
    JCExpression annotationFieldValue() {
        if (LAX_IDENTIFIER.accepts(token.kind)) {
            mode = EXPR;
            JCExpression t1 = term1();
            if (t1.hasTag(IDENT) && token.kind == EQ) {
                int pos = token.pos;
                accept(EQ);
                JCExpression v = annotationValue();
                return toP(F.at(pos).Assign(t1, v));
            } else {
                return t1;
            }
        }
        return annotationValue();
    }

    /**
     * HCZ:?
     *
     *  AnnotationValue          = ConditionalExpression
     *                          | Annotation
     *                          | "{" [ AnnotationValue { "," AnnotationValue } ] [","] "}"
     */
    JCExpression annotationValue() {
        int pos;
        switch (token.kind) {
        case MONKEYS_AT:
            pos = token.pos;
            //HCZ：nextToken()-？
            nextToken();
            return annotation(pos, Tag.ANNOTATION);
        case LBRACE:
            pos = token.pos;
            accept(LBRACE);
            ListBuffer<JCExpression> buf = new ListBuffer<JCExpression>();
            if (token.kind == COMMA) {
                //HCZ：nextToken()-？
                nextToken();
            } else if (token.kind != RBRACE) {
                buf.append(annotationValue());
                while (token.kind == COMMA) {
                    //HCZ：nextToken()-？
                    nextToken();
                    if (token.kind == RBRACE) break;
                    buf.append(annotationValue());
                }
            }
            accept(RBRACE);
            return toP(F.at(pos).NewArray(null, List.<JCExpression>nil(), buf.toList()));
        default:
            mode = EXPR;
            return term1();
        }
    }

    /**
     * HCZ:?
     *
     *  VariableDeclarators = VariableDeclarator { "," VariableDeclarator }
     */
    public <T extends ListBuffer<? super JCVariableDecl>> T variableDeclarators(JCModifiers mods,
                                                                         JCExpression type,
                                                                         T vdefs)
    {
        return variableDeclaratorsRest(token.pos, mods, type, ident(), false, null, vdefs);
    }

    /**
     * HCZ:?
     *
     *  VariableDeclaratorsRest = VariableDeclaratorRest { "," VariableDeclarator }
     *  ConstantDeclaratorsRest = ConstantDeclaratorRest { "," ConstantDeclarator }
     *
     *  @param reqInit  Is an initializer always required?
     *  @param dc       The documentation comment for the variable declarations, or null.
     */
    <T extends ListBuffer<? super JCVariableDecl>> T variableDeclaratorsRest(int pos,
                                                                     JCModifiers mods,
                                                                     JCExpression type,
                                                                     Name name,
                                                                     boolean reqInit,
                                                                     Comment dc,
                                                                     T vdefs)
    {
        vdefs.append(variableDeclaratorRest(pos, mods, type, name, reqInit, dc));
        while (token.kind == COMMA) {
            // All but last of multiple declarators subsume a comma
            storeEnd((JCTree)vdefs.last(), token.endPos);
            //HCZ：nextToken()-？
            nextToken();
            vdefs.append(variableDeclarator(mods, type, reqInit, dc));
        }
        return vdefs;
    }

    /**
     * HCZ:?
     *
     *  VariableDeclarator = Ident VariableDeclaratorRest
     *  ConstantDeclarator = Ident ConstantDeclaratorRest
     */
    JCVariableDecl variableDeclarator(JCModifiers mods, JCExpression type, boolean reqInit, Comment dc) {
        return variableDeclaratorRest(token.pos, mods, type, ident(), reqInit, dc);
    }

    /**
     * HCZ:?
     *
     *  VariableDeclaratorRest = BracketsOpt ["=" VariableInitializer]
     *  ConstantDeclaratorRest = BracketsOpt "=" VariableInitializer
     *
     *  @param reqInit  Is an initializer always required?
     *  @param dc       The documentation comment for the variable declarations, or null.
     */
    JCVariableDecl variableDeclaratorRest(int pos, JCModifiers mods, JCExpression type, Name name,
                                  boolean reqInit, Comment dc) {
        type = bracketsOpt(type);
        JCExpression init = null;
        if (token.kind == EQ) {
            //HCZ：nextToken()-？
            nextToken();
            init = variableInitializer();
        }
        else if (reqInit) syntaxError(token.pos, "expected", EQ);
        JCVariableDecl result =
            toP(F.at(pos).VarDef(mods, name, type, init));
        attach(result, dc);
        return result;
    }

    /**
     * HCZ:?
     *
     *  VariableDeclaratorId = Ident BracketsOpt
     */
    JCVariableDecl variableDeclaratorId(JCModifiers mods, JCExpression type) {
        return variableDeclaratorId(mods, type, false);
    }
    //where   HCZ:?
    JCVariableDecl variableDeclaratorId(JCModifiers mods, JCExpression type, boolean lambdaParameter) {
        int pos = token.pos;
        Name name;
        if (lambdaParameter && token.kind == UNDERSCORE) {
            log.error(pos, "underscore.as.identifier.in.lambda");
            name = token.name();
            //HCZ：nextToken()-？
            nextToken();
        } else {
            if (allowThisIdent) {
                JCExpression pn = qualident(false);
                if (pn.hasTag(Tag.IDENT) && ((JCIdent)pn).name != names._this) {
                    name = ((JCIdent)pn).name;
                } else {
                    if ((mods.flags & Flags.VARARGS) != 0) {
                        log.error(token.pos, "varargs.and.receiver");
                    }
                    if (token.kind == LBRACKET) {
                        log.error(token.pos, "array.and.receiver");
                    }
                    return toP(F.at(pos).ReceiverVarDef(mods, pn, type));
                }
            } else {
                name = ident();
            }
        }
        if ((mods.flags & Flags.VARARGS) != 0 &&
                token.kind == LBRACKET) {
            log.error(token.pos, "varargs.and.old.array.syntax");
        }
        type = bracketsOpt(type);
        return toP(F.at(pos).VarDef(mods, name, type, null));
    }

    /**
     * HCZ:?
     *
     *  Resources = Resource { ";" Resources }
     */
    List<JCTree> resources() {
        ListBuffer<JCTree> defs = new ListBuffer<JCTree>();
        defs.append(resource());
        while (token.kind == SEMI) {
            // All but last of multiple declarators must subsume a semicolon
            storeEnd(defs.last(), token.endPos);
            int semiColonPos = token.pos;
            //HCZ：nextToken()-？
            nextToken();
            if (token.kind == RPAREN) { // Optional trailing semicolon
                                       // after last resource
                break;
            }
            defs.append(resource());
        }
        return defs.toList();
    }

    /**
     * HCZ:?
     *
     *  Resource = VariableModifiersOpt Type VariableDeclaratorId = Expression
     */
    protected JCTree resource() {
        JCModifiers optFinal = optFinal(Flags.FINAL);
        JCExpression type = parseType();
        int pos = token.pos;
        Name ident = ident();
        return variableDeclaratorRest(pos, optFinal, type, ident, true, null);
    }

    /**
     * HCZ：？词法解析，获得抽象语法树
     *
     *  CompilationUnit = [ { "@" Annotation } PACKAGE Qualident ";"] {ImportDeclaration} {TypeDeclaration}
     */
    public JCTree.JCCompilationUnit parseCompilationUnit() {
        Token firstToken = token;
        JCExpression pid = null;
        JCModifiers mods = null;
        boolean consumedToplevelDoc = false;
        boolean seenImport = false;
        boolean seenPackage = false;
        List<JCAnnotation> packageAnnotations = List.nil();
        if (token.kind == MONKEYS_AT)
            mods = modifiersOpt();

        //HCZ：如果当前Token是"package"，则
        if (token.kind == PACKAGE) {
            seenPackage = true;
            if (mods != null) {
                checkNoMods(mods.flags);
                packageAnnotations = mods.annotations;
                mods = null;
            }
            //HCZ：nextToken()-当前Token是"package"，获得"包名1.包名2.包名3"的"包名1"
            nextToken();
            //HCZ：调用qualident()->ident()->nextToken()，获得完整的包名
            pid = qualident(false);
            //HCZ：
            accept(SEMI);
        }
        ListBuffer<JCTree> defs = new ListBuffer<JCTree>();
        boolean checkForImports = true;
        boolean firstTypeDecl = true;
        while (token.kind != EOF) {
            if (token.pos > 0 && token.pos <= endPosTable.errorEndPos) {
                // error recovery
                skip(checkForImports, false, false, false);
                if (token.kind == EOF)
                    break;
            }
            if (checkForImports && mods == null && token.kind == IMPORT) {
                seenImport = true;
                defs.append(importDeclaration());
            } else {
                Comment docComment = token.comment(CommentStyle.JAVADOC);
                if (firstTypeDecl && !seenImport && !seenPackage) {
                    docComment = firstToken.comment(CommentStyle.JAVADOC);
                    consumedToplevelDoc = true;
                }
                JCTree def = typeDeclaration(mods, docComment);
                if (def instanceof JCExpressionStatement)
                    def = ((JCExpressionStatement)def).expr;
                defs.append(def);
                if (def instanceof JCClassDecl)
                    checkForImports = false;
                mods = null;
                firstTypeDecl = false;
            }
        }
        JCTree.JCCompilationUnit toplevel = F.at(firstToken.pos).TopLevel(packageAnnotations, pid, defs.toList());
        if (!consumedToplevelDoc)
            attach(toplevel, firstToken.comment(CommentStyle.JAVADOC));
        if (defs.isEmpty())
            storeEnd(toplevel, S.prevToken().endPos);
        if (keepDocComments)
            toplevel.docComments = docComments;
        if (keepLineMap)
            toplevel.lineMap = S.getLineMap();
        this.endPosTable.setParser(null); // remove reference to parser
        toplevel.endPositions = this.endPosTable;
        return toplevel;
    }

    /**
     * HCZ:?
     *
     *  ImportDeclaration = IMPORT [ STATIC ] Ident { "." Ident } [ "." "*" ] ";"
     */
    JCTree importDeclaration() {
        int pos = token.pos;
        //HCZ：nextToken()-？
        nextToken();
        boolean importStatic = false;
        if (token.kind == STATIC) {
            checkStaticImports();
            importStatic = true;
            //HCZ：nextToken()-？
            nextToken();
        }
        JCExpression pid = toP(F.at(token.pos).Ident(ident()));
        do {
            int pos1 = token.pos;
            accept(DOT);
            if (token.kind == STAR) {
                pid = to(F.at(pos1).Select(pid, names.asterisk));
                //HCZ：nextToken()-？
                nextToken();
                break;
            } else {
                pid = toP(F.at(pos1).Select(pid, ident()));
            }
        } while (token.kind == DOT);
        accept(SEMI);
        return toP(F.at(pos).Import(pid, importStatic));
    }

    /**
     * HCZ:?
     *
     *  TypeDeclaration = ClassOrInterfaceOrEnumDeclaration
     *                  | ";"
     */
    JCTree typeDeclaration(JCModifiers mods, Comment docComment) {
        int pos = token.pos;
        if (mods == null && token.kind == SEMI) {
            //HCZ：nextToken()-？
            nextToken();
            return toP(F.at(pos).Skip());
        } else {
            return classOrInterfaceOrEnumDeclaration(modifiersOpt(mods), docComment);
        }
    }

    /**
     * HCZ:?
     *
     *  ClassOrInterfaceOrEnumDeclaration = ModifiersOpt
     *           (ClassDeclaration | InterfaceDeclaration | EnumDeclaration)
     *  @param mods     Any modifiers starting the class or interface declaration
     *  @param dc       The documentation comment for the class, or null.
     */
    JCStatement classOrInterfaceOrEnumDeclaration(JCModifiers mods, Comment dc) {
        if (token.kind == CLASS) {
            return classDeclaration(mods, dc);
        } else if (token.kind == INTERFACE) {
            return interfaceDeclaration(mods, dc);
        } else if (allowEnums) {
            if (token.kind == ENUM) {
                return enumDeclaration(mods, dc);
            } else {
                int pos = token.pos;
                List<JCTree> errs;
                if (LAX_IDENTIFIER.accepts(token.kind)) {
                    errs = List.<JCTree>of(mods, toP(F.at(pos).Ident(ident())));
                    setErrorEndPos(token.pos);
                } else {
                    errs = List.<JCTree>of(mods);
                }
                return toP(F.Exec(syntaxError(pos, errs, "expected3",
                                              CLASS, INTERFACE, ENUM)));
            }
        } else {
            if (token.kind == ENUM) {
                error(token.pos, "enums.not.supported.in.source", source.name);
                allowEnums = true;
                return enumDeclaration(mods, dc);
            }
            int pos = token.pos;
            List<JCTree> errs;
            if (LAX_IDENTIFIER.accepts(token.kind)) {
                errs = List.<JCTree>of(mods, toP(F.at(pos).Ident(ident())));
                setErrorEndPos(token.pos);
            } else {
                errs = List.<JCTree>of(mods);
            }
            return toP(F.Exec(syntaxError(pos, errs, "expected2",
                                          CLASS, INTERFACE)));
        }
    }

    /**
     * HCZ:?
     *
     *  ClassDeclaration = CLASS Ident TypeParametersOpt [EXTENDS Type]
     *                     [IMPLEMENTS TypeList] ClassBody
     *  @param mods    The modifiers starting the class declaration
     *  @param dc       The documentation comment for the class, or null.
     */
    protected JCClassDecl classDeclaration(JCModifiers mods, Comment dc) {
        int pos = token.pos;
        accept(CLASS);
        Name name = ident();

        List<JCTypeParameter> typarams = typeParametersOpt();

        JCExpression extending = null;
        if (token.kind == EXTENDS) {
            //HCZ：nextToken()-？
            nextToken();
            extending = parseType();
        }
        List<JCExpression> implementing = List.nil();
        if (token.kind == IMPLEMENTS) {
            //HCZ：nextToken()-？
            nextToken();
            implementing = typeList();
        }
        List<JCTree> defs = classOrInterfaceBody(name, false);
        JCClassDecl result = toP(F.at(pos).ClassDef(
            mods, name, typarams, extending, implementing, defs));
        attach(result, dc);
        return result;
    }

    /**
     * HCZ:?
     *
     *  InterfaceDeclaration = INTERFACE Ident TypeParametersOpt
     *                         [EXTENDS TypeList] InterfaceBody
     *  @param mods    The modifiers starting the interface declaration
     *  @param dc       The documentation comment for the interface, or null.
     */
    protected JCClassDecl interfaceDeclaration(JCModifiers mods, Comment dc) {
        int pos = token.pos;
        accept(INTERFACE);
        Name name = ident();

        List<JCTypeParameter> typarams = typeParametersOpt();

        List<JCExpression> extending = List.nil();
        if (token.kind == EXTENDS) {
            //HCZ：nextToken()-？
            nextToken();
            extending = typeList();
        }
        List<JCTree> defs = classOrInterfaceBody(name, true);
        JCClassDecl result = toP(F.at(pos).ClassDef(
            mods, name, typarams, null, extending, defs));
        attach(result, dc);
        return result;
    }

    /**
     * HCZ:?
     *
     *  EnumDeclaration = ENUM Ident [IMPLEMENTS TypeList] EnumBody
     *  @param mods    The modifiers starting the enum declaration
     *  @param dc       The documentation comment for the enum, or null.
     */
    protected JCClassDecl enumDeclaration(JCModifiers mods, Comment dc) {
        int pos = token.pos;
        accept(ENUM);
        Name name = ident();

        List<JCExpression> implementing = List.nil();
        if (token.kind == IMPLEMENTS) {
            //HCZ：nextToken()-？
            nextToken();
            implementing = typeList();
        }

        List<JCTree> defs = enumBody(name);
        mods.flags |= Flags.ENUM;
        JCClassDecl result = toP(F.at(pos).
            ClassDef(mods, name, List.<JCTypeParameter>nil(),
                null, implementing, defs));
        attach(result, dc);
        return result;
    }

    /**
     * HCZ:?
     *
     *  EnumBody = "{" { EnumeratorDeclarationList } [","]
     *                  [ ";" {ClassBodyDeclaration} ] "}"
     */
    List<JCTree> enumBody(Name enumName) {
        accept(LBRACE);
        ListBuffer<JCTree> defs = new ListBuffer<JCTree>();
        if (token.kind == COMMA) {
            //HCZ：nextToken()-？
            nextToken();
        } else if (token.kind != RBRACE && token.kind != SEMI) {
            defs.append(enumeratorDeclaration(enumName));
            while (token.kind == COMMA) {
                //HCZ：nextToken()-？
                nextToken();
                if (token.kind == RBRACE || token.kind == SEMI) break;
                defs.append(enumeratorDeclaration(enumName));
            }
            if (token.kind != SEMI && token.kind != RBRACE) {
                defs.append(syntaxError(token.pos, "expected3",
                                COMMA, RBRACE, SEMI));
                //HCZ：nextToken()-？
                nextToken();
            }
        }
        if (token.kind == SEMI) {
            //HCZ：nextToken()-？
            nextToken();
            while (token.kind != RBRACE && token.kind != EOF) {
                defs.appendList(classOrInterfaceBodyDeclaration(enumName,
                                                                false));
                if (token.pos <= endPosTable.errorEndPos) {
                    // error recovery
                   skip(false, true, true, false);
                }
            }
        }
        accept(RBRACE);
        return defs.toList();
    }

    /**
     * HCZ:?
     *
     *  EnumeratorDeclaration = AnnotationsOpt [TypeArguments] IDENTIFIER [ Arguments ] [ "{" ClassBody "}" ]
     */
    JCTree enumeratorDeclaration(Name enumName) {
        Comment dc = token.comment(CommentStyle.JAVADOC);
        int flags = Flags.PUBLIC|Flags.STATIC|Flags.FINAL|Flags.ENUM;
        if (token.deprecatedFlag()) {
            flags |= Flags.DEPRECATED;
        }
        int pos = token.pos;
        List<JCAnnotation> annotations = annotationsOpt(Tag.ANNOTATION);
        JCModifiers mods = F.at(annotations.isEmpty() ? Position.NOPOS : pos).Modifiers(flags, annotations);
        List<JCExpression> typeArgs = typeArgumentsOpt();
        int identPos = token.pos;
        Name name = ident();
        int createPos = token.pos;
        List<JCExpression> args = (token.kind == LPAREN)
            ? arguments() : List.<JCExpression>nil();
        JCClassDecl body = null;
        if (token.kind == LBRACE) {
            JCModifiers mods1 = F.at(Position.NOPOS).Modifiers(Flags.ENUM | Flags.STATIC);
            List<JCTree> defs = classOrInterfaceBody(names.empty, false);
            body = toP(F.at(identPos).AnonymousClassDef(mods1, defs));
        }
        if (args.isEmpty() && body == null)
            createPos = identPos;
        JCIdent ident = F.at(identPos).Ident(enumName);
        JCNewClass create = F.at(createPos).NewClass(null, typeArgs, ident, args, body);
        if (createPos != identPos)
            storeEnd(create, S.prevToken().endPos);
        ident = F.at(identPos).Ident(enumName);
        JCTree result = toP(F.at(pos).VarDef(mods, name, ident, create));
        attach(result, dc);
        return result;
    }

    /**
     * HCZ:?
     *
     *  TypeList = Type {"," Type}
     */
    List<JCExpression> typeList() {
        ListBuffer<JCExpression> ts = new ListBuffer<JCExpression>();
        ts.append(parseType());
        while (token.kind == COMMA) {
            //HCZ：nextToken()-？
            nextToken();
            ts.append(parseType());
        }
        return ts.toList();
    }

    /**
     * HCZ:?
     *
     *  ClassBody     = "{" {ClassBodyDeclaration} "}"
     *  InterfaceBody = "{" {InterfaceBodyDeclaration} "}"
     */
    List<JCTree> classOrInterfaceBody(Name className, boolean isInterface) {
        accept(LBRACE);
        if (token.pos <= endPosTable.errorEndPos) {
            // error recovery
            skip(false, true, false, false);
            if (token.kind == LBRACE)
                //HCZ：nextToken()-？
                nextToken();
        }
        ListBuffer<JCTree> defs = new ListBuffer<JCTree>();
        while (token.kind != RBRACE && token.kind != EOF) {
            defs.appendList(classOrInterfaceBodyDeclaration(className, isInterface));
            if (token.pos <= endPosTable.errorEndPos) {
               // error recovery
               skip(false, true, true, false);
           }
        }
        accept(RBRACE);
        return defs.toList();
    }

    /**
     * HCZ:?
     *
     *  ClassBodyDeclaration =
     *      ";"
     *    | [STATIC] Block
     *    | ModifiersOpt
     *      ( Type Ident
     *        ( VariableDeclaratorsRest ";" | MethodDeclaratorRest )
     *      | VOID Ident MethodDeclaratorRest
     *      | TypeParameters (Type | VOID) Ident MethodDeclaratorRest
     *      | Ident ConstructorDeclaratorRest
     *      | TypeParameters Ident ConstructorDeclaratorRest
     *      | ClassOrInterfaceOrEnumDeclaration
     *      )
     *  InterfaceBodyDeclaration =
     *      ";"
     *    | ModifiersOpt Type Ident
     *      ( ConstantDeclaratorsRest | InterfaceMethodDeclaratorRest ";" )
     */
    protected List<JCTree> classOrInterfaceBodyDeclaration(Name className, boolean isInterface) {
        if (token.kind == SEMI) {
            //HCZ：nextToken()-？
            nextToken();
            return List.<JCTree>nil();
        } else {
            Comment dc = token.comment(CommentStyle.JAVADOC);
            int pos = token.pos;
            JCModifiers mods = modifiersOpt();
            if (token.kind == CLASS ||
                token.kind == INTERFACE ||
                allowEnums && token.kind == ENUM) {
                return List.<JCTree>of(classOrInterfaceOrEnumDeclaration(mods, dc));
            } else if (token.kind == LBRACE && !isInterface &&
                       (mods.flags & Flags.StandardFlags & ~Flags.STATIC) == 0 &&
                       mods.annotations.isEmpty()) {
                return List.<JCTree>of(block(pos, mods.flags));
            } else {
                pos = token.pos;
                List<JCTypeParameter> typarams = typeParametersOpt();
                // if there are type parameters but no modifiers, save the start
                // position of the method in the modifiers.
                if (typarams.nonEmpty() && mods.pos == Position.NOPOS) {
                    mods.pos = pos;
                    storeEnd(mods, pos);
                }
                List<JCAnnotation> annosAfterParams = annotationsOpt(Tag.ANNOTATION);

                Token tk = token;
                pos = token.pos;
                JCExpression type;
                boolean isVoid = token.kind == VOID;
                if (isVoid) {
                    if (annosAfterParams.nonEmpty())
                        illegal(annosAfterParams.head.pos);
                    type = to(F.at(pos).TypeIdent(TypeTag.VOID));
                    //HCZ：nextToken()-？
                    nextToken();
                } else {
                    if (annosAfterParams.nonEmpty()) {
                        mods.annotations = mods.annotations.appendList(annosAfterParams);
                        if (mods.pos == Position.NOPOS)
                            mods.pos = mods.annotations.head.pos;
                    }
                    // method returns types are un-annotated types
                    type = unannotatedType();
                }
                if (token.kind == LPAREN && !isInterface && type.hasTag(IDENT)) {
                    if (isInterface || tk.name() != className)
                        error(pos, "invalid.meth.decl.ret.type.req");
                    return List.of(methodDeclaratorRest(
                        pos, mods, null, names.init, typarams,
                        isInterface, true, dc));
                } else {
                    pos = token.pos;
                    Name name = ident();
                    if (token.kind == LPAREN) {
                        return List.of(methodDeclaratorRest(
                            pos, mods, type, name, typarams,
                            isInterface, isVoid, dc));
                    } else if (!isVoid && typarams.isEmpty()) {
                        List<JCTree> defs =
                            variableDeclaratorsRest(pos, mods, type, name, isInterface, dc,
                                                    new ListBuffer<JCTree>()).toList();
                        storeEnd(defs.last(), token.endPos);
                        accept(SEMI);
                        return defs;
                    } else {
                        pos = token.pos;
                        List<JCTree> err = isVoid
                            ? List.<JCTree>of(toP(F.at(pos).MethodDef(mods, name, type, typarams,
                                List.<JCVariableDecl>nil(), List.<JCExpression>nil(), null, null)))
                            : null;
                        return List.<JCTree>of(syntaxError(token.pos, err, "expected", LPAREN));
                    }
                }
            }
        }
    }

    /**
     * HCZ:?
     *
     *  MethodDeclaratorRest =
     *      FormalParameters BracketsOpt [Throws TypeList] ( MethodBody | [DEFAULT AnnotationValue] ";")
     *  VoidMethodDeclaratorRest =
     *      FormalParameters [Throws TypeList] ( MethodBody | ";")
     *  InterfaceMethodDeclaratorRest =
     *      FormalParameters BracketsOpt [THROWS TypeList] ";"
     *  VoidInterfaceMethodDeclaratorRest =
     *      FormalParameters [THROWS TypeList] ";"
     *  ConstructorDeclaratorRest =
     *      "(" FormalParameterListOpt ")" [THROWS TypeList] MethodBody
     */
    protected JCTree methodDeclaratorRest(int pos,
                              JCModifiers mods,
                              JCExpression type,
                              Name name,
                              List<JCTypeParameter> typarams,
                              boolean isInterface, boolean isVoid,
                              Comment dc) {
        if (isInterface && (mods.flags & Flags.STATIC) != 0) {
            checkStaticInterfaceMethods();
        }
        JCVariableDecl prevReceiverParam = this.receiverParam;
        try {
            this.receiverParam = null;
            // Parsing formalParameters sets the receiverParam, if present
            List<JCVariableDecl> params = formalParameters();
            if (!isVoid) type = bracketsOpt(type);
            List<JCExpression> thrown = List.nil();
            if (token.kind == THROWS) {
                //HCZ：nextToken()-？
                nextToken();
                thrown = qualidentList();
            }
            JCBlock body = null;
            JCExpression defaultValue;
            if (token.kind == LBRACE) {
                body = block();
                defaultValue = null;
            } else {
                if (token.kind == DEFAULT) {
                    accept(DEFAULT);
                    defaultValue = annotationValue();
                } else {
                    defaultValue = null;
                }
                accept(SEMI);
                if (token.pos <= endPosTable.errorEndPos) {
                    // error recovery
                    skip(false, true, false, false);
                    if (token.kind == LBRACE) {
                        body = block();
                    }
                }
            }

            JCMethodDecl result =
                    toP(F.at(pos).MethodDef(mods, name, type, typarams,
                                            receiverParam, params, thrown,
                                            body, defaultValue));
            attach(result, dc);
            return result;
        } finally {
            this.receiverParam = prevReceiverParam;
        }
    }

    /**
     * HCZ:?
     *
     *  QualidentList = [Annotations] Qualident {"," [Annotations] Qualident}
     */
    List<JCExpression> qualidentList() {
        ListBuffer<JCExpression> ts = new ListBuffer<JCExpression>();

        List<JCAnnotation> typeAnnos = typeAnnotationsOpt();
        JCExpression qi = qualident(true);
        if (!typeAnnos.isEmpty()) {
            JCExpression at = insertAnnotationsToMostInner(qi, typeAnnos, false);
            ts.append(at);
        } else {
            ts.append(qi);
        }
        while (token.kind == COMMA) {
            //HCZ：nextToken()-？
            nextToken();

            typeAnnos = typeAnnotationsOpt();
            qi = qualident(true);
            if (!typeAnnos.isEmpty()) {
                JCExpression at = insertAnnotationsToMostInner(qi, typeAnnos, false);
                ts.append(at);
            } else {
                ts.append(qi);
            }
        }
        return ts.toList();
    }

    /**
     * HCZ:?
     *
     *  {@literal
     *  TypeParametersOpt = ["<" TypeParameter {"," TypeParameter} ">"]
     *  }
     */
    List<JCTypeParameter> typeParametersOpt() {
        if (token.kind == LT) {
            checkGenerics();
            ListBuffer<JCTypeParameter> typarams = new ListBuffer<JCTypeParameter>();
            //HCZ：nextToken()-？
            nextToken();
            typarams.append(typeParameter());
            while (token.kind == COMMA) {
                //HCZ：nextToken()-？
                nextToken();
                typarams.append(typeParameter());
            }
            accept(GT);
            return typarams.toList();
        } else {
            return List.nil();
        }
    }

    /**
     * HCZ:?
     *
     *  {@literal
     *  TypeParameter = [Annotations] TypeVariable [TypeParameterBound]
     *  TypeParameterBound = EXTENDS Type {"&" Type}
     *  TypeVariable = Ident
     *  }
     */
    JCTypeParameter typeParameter() {
        int pos = token.pos;
        List<JCAnnotation> annos = typeAnnotationsOpt();
        Name name = ident();
        ListBuffer<JCExpression> bounds = new ListBuffer<JCExpression>();
        if (token.kind == EXTENDS) {
            //HCZ：nextToken()-？
            nextToken();
            bounds.append(parseType());
            while (token.kind == AMP) {
                //HCZ：nextToken()-？
                nextToken();
                bounds.append(parseType());
            }
        }
        return toP(F.at(pos).TypeParameter(name, bounds.toList(), annos));
    }

    /**
     * HCZ:?
     *
     *  FormalParameters = "(" [ FormalParameterList ] ")"
     *  FormalParameterList = [ FormalParameterListNovarargs , ] LastFormalParameter
     *  FormalParameterListNovarargs = [ FormalParameterListNovarargs , ] FormalParameter
     */
    List<JCVariableDecl> formalParameters() {
        return formalParameters(false);
    }
    /**
     * HCZ:?
     */
    List<JCVariableDecl> formalParameters(boolean lambdaParameters) {
        ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
        JCVariableDecl lastParam;
        accept(LPAREN);
        if (token.kind != RPAREN) {
            this.allowThisIdent = true;
            lastParam = formalParameter(lambdaParameters);
            if (lastParam.nameexpr != null) {
                this.receiverParam = lastParam;
            } else {
                params.append(lastParam);
            }
            this.allowThisIdent = false;
            while ((lastParam.mods.flags & Flags.VARARGS) == 0 && token.kind == COMMA) {
                //HCZ：nextToken()-？
                nextToken();
                params.append(lastParam = formalParameter(lambdaParameters));
            }
        }
        accept(RPAREN);
        return params.toList();
    }
    /**
     * HCZ:?
     */
    List<JCVariableDecl> implicitParameters(boolean hasParens) {
        if (hasParens) {
            accept(LPAREN);
        }
        ListBuffer<JCVariableDecl> params = new ListBuffer<JCVariableDecl>();
        if (token.kind != RPAREN && token.kind != ARROW) {
            params.append(implicitParameter());
            while (token.kind == COMMA) {
                //HCZ：nextToken()-？
                nextToken();
                params.append(implicitParameter());
            }
        }
        if (hasParens) {
            accept(RPAREN);
        }
        return params.toList();
    }
    /**
     * HCZ:?
     */
    JCModifiers optFinal(long flags) {
        JCModifiers mods = modifiersOpt();
        checkNoMods(mods.flags & ~(Flags.FINAL | Flags.DEPRECATED));
        mods.flags |= flags;
        return mods;
    }

    /**
     * HCZ:?
     *
     * Inserts the annotations (and possibly a new array level)
     * to the left-most type in an array or nested type.
     *
     * When parsing a type like {@code @B Outer.Inner @A []}, the
     * {@code @A} annotation should target the array itself, while
     * {@code @B} targets the nested type {@code Outer}.
     *
     * Currently the parser parses the annotation first, then
     * the array, and then inserts the annotation to the left-most
     * nested type.
     *
     * When {@code createNewLevel} is true, then a new array
     * level is inserted as the most inner type, and have the
     * annotations target it.  This is useful in the case of
     * varargs, e.g. {@code String @A [] @B ...}, as the parser
     * first parses the type {@code String @A []} then inserts
     * a new array level with {@code @B} annotation.
     */
    private JCExpression insertAnnotationsToMostInner(
            JCExpression type, List<JCAnnotation> annos,
            boolean createNewLevel) {
        int origEndPos = getEndPos(type);
        JCExpression mostInnerType = type;
        JCArrayTypeTree mostInnerArrayType = null;
        while (TreeInfo.typeIn(mostInnerType).hasTag(TYPEARRAY)) {
            mostInnerArrayType = (JCArrayTypeTree) TreeInfo.typeIn(mostInnerType);
            mostInnerType = mostInnerArrayType.elemtype;
        }

        if (createNewLevel) {
            mostInnerType = to(F.at(token.pos).TypeArray(mostInnerType));
        }

        JCExpression mostInnerTypeToReturn = mostInnerType;
        if (annos.nonEmpty()) {
            JCExpression lastToModify = mostInnerType;

            while (TreeInfo.typeIn(mostInnerType).hasTag(SELECT) ||
                    TreeInfo.typeIn(mostInnerType).hasTag(TYPEAPPLY)) {
                while (TreeInfo.typeIn(mostInnerType).hasTag(SELECT)) {
                    lastToModify = mostInnerType;
                    mostInnerType = ((JCFieldAccess) TreeInfo.typeIn(mostInnerType)).getExpression();
                }
                while (TreeInfo.typeIn(mostInnerType).hasTag(TYPEAPPLY)) {
                    lastToModify = mostInnerType;
                    mostInnerType = ((JCTypeApply) TreeInfo.typeIn(mostInnerType)).clazz;
                }
            }

            mostInnerType = F.at(annos.head.pos).AnnotatedType(annos, mostInnerType);

            if (TreeInfo.typeIn(lastToModify).hasTag(TYPEAPPLY)) {
                ((JCTypeApply) TreeInfo.typeIn(lastToModify)).clazz = mostInnerType;
            } else if (TreeInfo.typeIn(lastToModify).hasTag(SELECT)) {
                ((JCFieldAccess) TreeInfo.typeIn(lastToModify)).selected = mostInnerType;
            } else {
                // We never saw a SELECT or TYPEAPPLY, return the annotated type.
                mostInnerTypeToReturn = mostInnerType;
            }
        }

        if (mostInnerArrayType == null) {
            return mostInnerTypeToReturn;
        } else {
            mostInnerArrayType.elemtype = mostInnerTypeToReturn;
            storeEnd(type, origEndPos);
            return type;
        }
    }

    /**
     * HCZ:?
     *
     *  FormalParameter = { FINAL | '@' Annotation } Type VariableDeclaratorId
     *  LastFormalParameter = { FINAL | '@' Annotation } Type '...' Ident | FormalParameter
     */
    protected JCVariableDecl formalParameter() {
        return formalParameter(false);
    }
    /**
     * HCZ:?
     */
    protected JCVariableDecl formalParameter(boolean lambdaParameter) {
        JCModifiers mods = optFinal(Flags.PARAMETER);
        // need to distinguish between vararg annos and array annos
        // look at typeAnnotationsPushedBack comment
        this.permitTypeAnnotationsPushBack = true;
        JCExpression type = parseType();
        this.permitTypeAnnotationsPushBack = false;

        if (token.kind == ELLIPSIS) {
            List<JCAnnotation> varargsAnnos = typeAnnotationsPushedBack;
            typeAnnotationsPushedBack = List.nil();
            checkVarargs();
            mods.flags |= Flags.VARARGS;
            // insert var arg type annotations
            type = insertAnnotationsToMostInner(type, varargsAnnos, true);
            //HCZ：nextToken()-？
            nextToken();
        } else {
            // if not a var arg, then typeAnnotationsPushedBack should be null
            if (typeAnnotationsPushedBack.nonEmpty()) {
                reportSyntaxError(typeAnnotationsPushedBack.head.pos,
                        "illegal.start.of.type");
            }
            typeAnnotationsPushedBack = List.nil();
        }
        return variableDeclaratorId(mods, type, lambdaParameter);
    }
    /**
     * HCZ:?
     */
    protected JCVariableDecl implicitParameter() {
        JCModifiers mods = F.at(token.pos).Modifiers(Flags.PARAMETER);
        return variableDeclaratorId(mods, null, true);
    }

/* ---------- auxiliary methods -------------- */

    /**
     * HCZ:?
     */
    void error(int pos, String key, Object ... args) {
        log.error(DiagnosticFlag.SYNTAX, pos, key, args);
    }
    /**
     * HCZ:?
     */
    void error(DiagnosticPosition pos, String key, Object ... args) {
        log.error(DiagnosticFlag.SYNTAX, pos, key, args);
    }
    /**
     * HCZ:?
     */
    void warning(int pos, String key, Object ... args) {
        log.warning(pos, key, args);
    }

    /**
     * HCZ:?
     *
     *  Check that given tree is a legal expression statement.
     */
    protected JCExpression checkExprStat(JCExpression t) {
        if (!TreeInfo.isExpressionStatement(t)) {
            JCExpression ret = F.at(t.pos).Erroneous(List.<JCTree>of(t));
            error(ret, "not.stmt");
            return ret;
        } else {
            return t;
        }
    }

    /**
     * HCZ:?
     *
     *  Return precedence of operator represented by token,
     *  -1 if token is not a binary operator. @see TreeInfo.opPrec
     */
    static int prec(TokenKind token) {
        JCTree.Tag oc = optag(token);
        return (oc != NO_TAG) ? TreeInfo.opPrec(oc) : -1;
    }

    /**
     * HCZ:?
     *
     * Return the lesser of two positions, making allowance for either one
     * being unset.
     */
    static int earlier(int pos1, int pos2) {
        if (pos1 == Position.NOPOS)
            return pos2;
        if (pos2 == Position.NOPOS)
            return pos1;
        return (pos1 < pos2 ? pos1 : pos2);
    }

    /**
     * HCZ:?
     *
     *  Return operation tag of binary operator represented by token,
     *  No_TAG if token is not a binary operator.
     */
    static JCTree.Tag optag(TokenKind token) {
        switch (token) {
        case BARBAR:
            return OR;
        case AMPAMP:
            return AND;
        case BAR:
            return BITOR;
        case BAREQ:
            return BITOR_ASG;
        case CARET:
            return BITXOR;
        case CARETEQ:
            return BITXOR_ASG;
        case AMP:
            return BITAND;
        case AMPEQ:
            return BITAND_ASG;
        case EQEQ:
            return JCTree.Tag.EQ;
        case BANGEQ:
            return NE;
        case LT:
            return JCTree.Tag.LT;
        case GT:
            return JCTree.Tag.GT;
        case LTEQ:
            return LE;
        case GTEQ:
            return GE;
        case LTLT:
            return SL;
        case LTLTEQ:
            return SL_ASG;
        case GTGT:
            return SR;
        case GTGTEQ:
            return SR_ASG;
        case GTGTGT:
            return USR;
        case GTGTGTEQ:
            return USR_ASG;
        case PLUS:
            return JCTree.Tag.PLUS;
        case PLUSEQ:
            return PLUS_ASG;
        case SUB:
            return MINUS;
        case SUBEQ:
            return MINUS_ASG;
        case STAR:
            return MUL;
        case STAREQ:
            return MUL_ASG;
        case SLASH:
            return DIV;
        case SLASHEQ:
            return DIV_ASG;
        case PERCENT:
            return MOD;
        case PERCENTEQ:
            return MOD_ASG;
        case INSTANCEOF:
            return TYPETEST;
        default:
            return NO_TAG;
        }
    }

    /**
     * HCZ:?
     *
     *  Return operation tag of unary operator represented by token,
     *  No_TAG if token is not a binary operator.
     */
    static JCTree.Tag unoptag(TokenKind token) {
        switch (token) {
        case PLUS:
            return POS;
        case SUB:
            return NEG;
        case BANG:
            return NOT;
        case TILDE:
            return COMPL;
        case PLUSPLUS:
            return PREINC;
        case SUBSUB:
            return PREDEC;
        default:
            return NO_TAG;
        }
    }

    /**
     * HCZ:?
     *
     *  Return type tag of basic type represented by token,
     *  NONE if token is not a basic type identifier.
     */
    static TypeTag typetag(TokenKind token) {
        switch (token) {
        case BYTE:
            return TypeTag.BYTE;
        case CHAR:
            return TypeTag.CHAR;
        case SHORT:
            return TypeTag.SHORT;
        case INT:
            return TypeTag.INT;
        case LONG:
            return TypeTag.LONG;
        case FLOAT:
            return TypeTag.FLOAT;
        case DOUBLE:
            return TypeTag.DOUBLE;
        case BOOLEAN:
            return TypeTag.BOOLEAN;
        default:
            return TypeTag.NONE;
        }
    }

    /**
     * HCZ:?
     */
    void checkGenerics() {
        if (!allowGenerics) {
            error(token.pos, "generics.not.supported.in.source", source.name);
            allowGenerics = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkVarargs() {
        if (!allowVarargs) {
            error(token.pos, "varargs.not.supported.in.source", source.name);
            allowVarargs = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkForeach() {
        if (!allowForeach) {
            error(token.pos, "foreach.not.supported.in.source", source.name);
            allowForeach = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkStaticImports() {
        if (!allowStaticImport) {
            error(token.pos, "static.import.not.supported.in.source", source.name);
            allowStaticImport = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkAnnotations() {
        if (!allowAnnotations) {
            error(token.pos, "annotations.not.supported.in.source", source.name);
            allowAnnotations = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkDiamond() {
        if (!allowDiamond) {
            error(token.pos, "diamond.not.supported.in.source", source.name);
            allowDiamond = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkMulticatch() {
        if (!allowMulticatch) {
            error(token.pos, "multicatch.not.supported.in.source", source.name);
            allowMulticatch = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkTryWithResources() {
        if (!allowTWR) {
            error(token.pos, "try.with.resources.not.supported.in.source", source.name);
            allowTWR = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkLambda() {
        if (!allowLambda) {
            log.error(token.pos, "lambda.not.supported.in.source", source.name);
            allowLambda = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkMethodReferences() {
        if (!allowMethodReferences) {
            log.error(token.pos, "method.references.not.supported.in.source", source.name);
            allowMethodReferences = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkDefaultMethods() {
        if (!allowDefaultMethods) {
            log.error(token.pos, "default.methods.not.supported.in.source", source.name);
            allowDefaultMethods = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkIntersectionTypesInCast() {
        if (!allowIntersectionTypesInCast) {
            log.error(token.pos, "intersection.types.in.cast.not.supported.in.source", source.name);
            allowIntersectionTypesInCast = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkStaticInterfaceMethods() {
        if (!allowStaticInterfaceMethods) {
            log.error(token.pos, "static.intf.methods.not.supported.in.source", source.name);
            allowStaticInterfaceMethods = true;
        }
    }
    /**
     * HCZ:?
     */
    void checkTypeAnnotations() {
        if (!allowTypeAnnotations) {
            log.error(token.pos, "type.annotations.not.supported.in.source", source.name);
            allowTypeAnnotations = true;
        }
    }

    /**
     * HCZ:?
     *
     * a functional source tree and end position mappings
     */
    protected static class SimpleEndPosTable extends AbstractEndPosTable {

        private final Map<JCTree, Integer> endPosMap;

        SimpleEndPosTable(JavacParser parser) {
            super(parser);
            endPosMap = new HashMap<JCTree, Integer>();
        }

        public void storeEnd(JCTree tree, int endpos) {
            endPosMap.put(tree, errorEndPos > endpos ? errorEndPos : endpos);
        }

        protected <T extends JCTree> T to(T t) {
            storeEnd(t, parser.token.endPos);
            return t;
        }

        protected <T extends JCTree> T toP(T t) {
            storeEnd(t, parser.S.prevToken().endPos);
            return t;
        }

        public int getEndPos(JCTree tree) {
            Integer value = endPosMap.get(tree);
            return (value == null) ? Position.NOPOS : value;
        }

        public int replaceTree(JCTree oldTree, JCTree newTree) {
            Integer pos = endPosMap.remove(oldTree);
            if (pos != null) {
                endPosMap.put(newTree, pos);
                return pos;
            }
            return Position.NOPOS;
        }
    }

    /**
     * HCZ:?
     *
     * a default skeletal implementation without any mapping overhead.
     */
    protected static class EmptyEndPosTable extends AbstractEndPosTable {

        EmptyEndPosTable(JavacParser parser) {
            super(parser);
        }

        public void storeEnd(JCTree tree, int endpos) { /* empty */ }

        protected <T extends JCTree> T to(T t) {
            return t;
        }

        protected <T extends JCTree> T toP(T t) {
            return t;
        }

        public int getEndPos(JCTree tree) {
            return Position.NOPOS;
        }

        public int replaceTree(JCTree oldTree, JCTree newTree) {
            return Position.NOPOS;
        }

    }

    /**
     * HCZ:?
     */
    protected static abstract class AbstractEndPosTable implements EndPosTable {
        /**
         * The current parser.
         */
        protected JavacParser parser;

        /**
         * Store the last error position.
         */
        protected int errorEndPos;

        public AbstractEndPosTable(JavacParser parser) {
            this.parser = parser;
        }

        /**
         * Store current token's ending position for a tree, the value of which
         * will be the greater of last error position and the ending position of
         * the current token.
         * @param t The tree.
         */
        protected abstract <T extends JCTree> T to(T t);

        /**
         * Store current token's ending position for a tree, the value of which
         * will be the greater of last error position and the ending position of
         * the previous token.
         * @param t The tree.
         */
        protected abstract <T extends JCTree> T toP(T t);

        /**
         * Set the error position during the parsing phases, the value of which
         * will be set only if it is greater than the last stored error position.
         * @param errPos The error position
         */
        protected void setErrorEndPos(int errPos) {
            if (errPos > errorEndPos) {
                errorEndPos = errPos;
            }
        }

        protected void setParser(JavacParser parser) {
            this.parser = parser;
        }
    }
}
