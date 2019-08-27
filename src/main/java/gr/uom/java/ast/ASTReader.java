package gr.uom.java.ast;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.lang.jvm.types.JvmReferenceType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
/*import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;*/

import com.intellij.psi.impl.source.PsiFieldImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import distance.ProjectInfo;
import gr.uom.java.ast.decomposition.AbstractExpression;
import gr.uom.java.ast.decomposition.MethodBodyObject;
import gr.uom.java.ast.util.StatementExtractor;

import java.util.*;

public class ASTReader {

    private static SystemObject systemObject;
    private static ProjectInfo examinedProject;
    //  public static final int JLS = AST.JLS4;

    public ASTReader(ProjectInfo project/*, IProgressMonitor monitor*/) /*throws CompilationErrorDetectedException */ {
/*		List<IMarker> markers = buildProject(iJavaProject, monitor);
		if(!markers.isEmpty()) {
			throw new CompilationErrorDetectedException(markers);
		}*/
/*		if(monitor != null)
			monitor.beginTask("Parsing selected Java Project", getNumberOfCompilationUnits(iJavaProject));*/
        systemObject = new SystemObject();
        examinedProject = project;
        List<PsiClass> classes = project.getClasses();
        for (PsiClass c : classes) {
            systemObject.addClass(processTypeDeclaration(c));
        }

        /*	try {*/
/*			IPackageFragmentRoot[] iPackageFragmentRoots = iJavaProject.getPackageFragmentRoots();
			for(IPackageFragmentRoot iPackageFragmentRoot : iPackageFragmentRoots) {
				IJavaElement[] children = iPackageFragmentRoot.getChildren();*/
/*        for (IJavaElement child : children) {
            if (child.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
                IPackageFragment iPackageFragment = (IPackageFragment) child;
                ICompilationUnit[] iCompilationUnits = iPackageFragment.getCompilationUnits();
                for (ICompilationUnit iCompilationUnit : iCompilationUnits) {
*//*							if(monitor != null && monitor.isCanceled())
				    			throw new OperationCanceledException();*//*
                    systemObject.addClasses(parseAST(iCompilationUnit));
*//*							if(monitor != null)
								monitor.worked(1);*//*
                }
            }
        }*/
    }
/*		} catch (JavaModelException e) {
			e.printStackTrace();
		}*/
/*		if(monitor != null)
			monitor.done();*/
    //}

    public ASTReader(ProjectInfo iJavaProject, SystemObject existingSystemObject) throws CompilationErrorDetectedException {
        // List<IMarker> markers = buildProject(iJavaProject);
/*        if (!markers.isEmpty()) {
            throw new CompilationErrorDetectedException(markers);
        }*/
       /* Set<ICompilationUnit> changedCompilationUnits = new LinkedHashSet<ICompilationUnit>();
        Set<ICompilationUnit> addedCompilationUnits = new LinkedHashSet<ICompilationUnit>();
        Set<ICompilationUnit> removedCompilationUnits = new LinkedHashSet<ICompilationUnit>();
        CompilationUnitCache instance = CompilationUnitCache.getInstance();
        for (ICompilationUnit changedCompilationUnit : instance.getChangedCompilationUnits()) {
            if (changedCompilationUnit.getJavaProject().equals(iJavaProject))
                changedCompilationUnits.add(changedCompilationUnit);
        }
        for (ICompilationUnit addedCompilationUnit : instance.getAddedCompilationUnits()) {
            if (addedCompilationUnit.getJavaProject().equals(iJavaProject))
                addedCompilationUnits.add(addedCompilationUnit);
        }
        for (ICompilationUnit removedCompilationUnit : instance.getRemovedCompilationUnits()) {
            if (removedCompilationUnit.getJavaProject().equals(iJavaProject))
                removedCompilationUnits.add(removedCompilationUnit);
        }*/
/*        if (monitor != null)
            monitor.beginTask("Parsing changed/added Compilation Units",
                    changedCompilationUnits.size() + addedCompilationUnits.size());*/
        examinedProject = iJavaProject;
        systemObject = existingSystemObject;
        List<PsiClass> classes = iJavaProject.getClasses();
        for (PsiClass c : classes) {
            systemObject.addClass(processTypeDeclaration(c));
        }
        

/*        for (ICompilationUnit removedCompilationUnit : removedCompilationUnits) {
            IFile removedCompilationUnitFile = (IFile) removedCompilationUnit.getResource();
            systemObject.removeClasses(removedCompilationUnitFile);
        }
        for (ICompilationUnit changedCompilationUnit : changedCompilationUnits) {
            List<ClassObject> changedClassObjects = parseAST(changedCompilationUnit);
            for (ClassObject changedClassObject : changedClassObjects) {
                systemObject.replaceClass(changedClassObject);
            }*/
/*            if (monitor != null)
                monitor.worked(1);*/
/*        }
        for (ICompilationUnit addedCompilationUnit : addedCompilationUnits) {
            List<ClassObject> addedClassObjects = parseAST(addedCompilationUnit);
            for (ClassObject addedClassObject : addedClassObjects) {
                systemObject.addClass(addedClassObject);
            }
*//*            if (monitor != null)
                monitor.worked(1);*//*
        }
        instance.clearAffectedCompilationUnits();*/
/*        if (monitor != null)
            monitor.done();*/
    }

    private ClassObject processTypeDeclaration(PsiClass psiClass) {
        final ClassObject classObject = new ClassObject(psiClass);
        classObject.setIFile(psiClass.getContainingFile());
        // classObject.addComments(processComments(psiClass.getContainingFile(), psiClass, psiClass.getDocComment()));
        classObject.setName(psiClass.getQualifiedName());
        //   classObject.setAbstractTypeDeclaration(psiClass);
        if (psiClass.isInterface()) {
            classObject.setInterface(true);
        }
        if ((psiClass.hasModifier(JvmModifier.ABSTRACT)))
            classObject.setAbstract(true);

        if ((psiClass.hasModifier(JvmModifier.PUBLIC)))
            classObject.setAccess(Access.PUBLIC);
        else if ((psiClass.hasModifier(JvmModifier.PROTECTED)))
            classObject.setAccess(Access.PROTECTED);
        else if ((psiClass.hasModifier(JvmModifier.PRIVATE)))
            classObject.setAccess(Access.PRIVATE);
        else
            classObject.setAccess(Access.NONE);

        if ((psiClass.hasModifier(JvmModifier.STATIC)))
            classObject.setStatic(true);

        JvmReferenceType superclassType = psiClass.getSuperClassType();
        if (superclassType != null) {
            String qualifiedName = superclassType.getName();
            TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
            classObject.setSuperclass(typeObject);
        }

        List<JvmReferenceType> superInterfaceTypes = Arrays.asList(psiClass.getInterfaceTypes());
        for (JvmReferenceType interfaceType : superInterfaceTypes) {
            TypeObject typeObject = TypeObject.extractTypeObject(interfaceType.getName());
            classObject.addInterface(typeObject);
        }

        PsiField[] fieldDeclarations = psiClass.getFields();
        for (PsiField fieldDeclaration : fieldDeclarations) {
            processFieldDeclaration(classObject, fieldDeclaration);
        }

        PsiMethod[] methodDeclarations = psiClass.getMethods();
        for (PsiMethod methodDeclaration : methodDeclarations) {
            processMethodDeclaration(classObject, methodDeclaration);
        }
        return classObject;
    }

    private void processFieldDeclaration(final ClassObject classObject, PsiField fieldDeclaration) {
        PsiType fieldType = fieldDeclaration.getType();
        //  ITypeBinding binding = fieldType.resolveBinding();
        List<CommentObject> fieldDeclarationComments = new ArrayList<CommentObject>();
        int fieldDeclarationStartPosition = fieldDeclaration.getStartOffsetInParent();
        int fieldDeclarationEndPosition = fieldDeclarationStartPosition + fieldDeclaration.getTextLength();
        for (CommentObject comment : classObject.commentList) {
            int commentStartPosition = comment.getStartPosition();
            int commentEndPosition = commentStartPosition + comment.getLength();
            if (fieldDeclarationStartPosition <= commentStartPosition && fieldDeclarationEndPosition >= commentEndPosition) {
                fieldDeclarationComments.add(comment);
            }
        }
/*        List<VariableDeclarationFragment> fragments = fieldDeclaration;
        for (VariableDeclarationFragment fragment : fragments) {*/
            TypeObject typeObject = TypeObject.extractTypeObject(fieldDeclaration.getType().getCanonicalText());
            typeObject.setArrayDimension(typeObject.getArrayDimension() /*+ fieldDeclaration.getExtraDimensions()*/);
            FieldObject fieldObject = new FieldObject(typeObject, fieldDeclaration);
            fieldObject.setClassName(classObject.getName());
           // fieldObject.setVariableDeclarationFragment(fragment);
            fieldObject.addComments(fieldDeclarationComments);

            if (fieldDeclaration.hasModifier(JvmModifier.PUBLIC))
                fieldObject.setAccess(Access.PUBLIC);
            else if (fieldDeclaration.hasModifier(JvmModifier.PROTECTED))
                fieldObject.setAccess(Access.PROTECTED);
            else if (fieldDeclaration.hasModifier(JvmModifier.PRIVATE))
                fieldObject.setAccess(Access.PRIVATE);
            else
                fieldObject.setAccess(Access.NONE);
            if (fieldDeclaration.hasModifier(JvmModifier.STATIC))
                fieldObject.setStatic(true);

            classObject.addField(fieldObject);
        
    }

    private void processMethodDeclaration(final ClassObject classObject, PsiMethod methodDeclaration) {
        String methodName = methodDeclaration.getName();
        final ConstructorObject constructorObject = new ConstructorObject();
        constructorObject.setMethodDeclaration(methodDeclaration);
        constructorObject.setName(methodName);
        constructorObject.setClassName(classObject.getName());
        int methodDeclarationStartPosition = methodDeclaration.getStartOffsetInParent();
        int methodDeclarationEndPosition = methodDeclarationStartPosition + methodDeclaration.getTextLength();
        for (CommentObject comment : classObject.commentList) {
            int commentStartPosition = comment.getStartPosition();
            int commentEndPosition = commentStartPosition + comment.getLength();
            if (methodDeclarationStartPosition <= commentStartPosition && methodDeclarationEndPosition >= commentEndPosition) {
                constructorObject.addComment(comment);
            }
        }

/*        if (methodDeclaration.getDocComment() != null) {
            PsiDocComment javaDoc = methodDeclaration.getJavadoc();
            List<PsiDocTag> tags = Arrays.asList(javaDoc.getTags());
            for (PsiDocTag tagElement : tags) {
                String tagName = tagElement.getName();
                if (tagName != null && tagName.equals(TagElement.TAG_THROWS)) {
                    List<ASTNode> fragments = tagElement.();
                    for (ASTNode docElement : fragments) {
                        if (docElement instanceof Name) {
                            Name name = (Name) docElement;
                            IBinding binding = name.resolveBinding();
                            if (binding instanceof ITypeBinding) {
                                ITypeBinding typeBinding = (ITypeBinding) binding;
                                constructorObject.addExceptionInJavaDocThrows(typeBinding.getQualifiedName());
                            }
                        }
                    }
                }
            }
        }*/
       // int methodModifiers = methodDeclaration.getModifiers();
        if (methodDeclaration.hasModifier(JvmModifier.PUBLIC))
            constructorObject.setAccess(Access.PUBLIC);
        else if (methodDeclaration.hasModifier(JvmModifier.PROTECTED))
            constructorObject.setAccess(Access.PROTECTED);
        else if (methodDeclaration.hasModifier(JvmModifier.PRIVATE))
            constructorObject.setAccess(Access.PRIVATE);
        else
            constructorObject.setAccess(Access.NONE);

        List<PsiParameter> parameters = Arrays.asList(methodDeclaration.getParameterList().getParameters());
        for (PsiParameter parameter : parameters) {
            PsiType parameterType = parameter.getType();
            String qualifiedName = parameter.getName();
            TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
          //  typeObject.setArrayDimension(typeObject.getArrayDimension() + parameter.getExtraDimensions());
            if (parameter.isVarArgs()) {
                typeObject.setArrayDimension(1);
            }
            ParameterObject parameterObject = new ParameterObject(typeObject, parameter.getName(), parameter.isVarArgs());
            parameterObject.setSingleVariableDeclaration(parameter);
            constructorObject.addParameter(parameterObject);
        }

        PsiCodeBlock methodBody = methodDeclaration.getBody();
        if (methodBody != null) {
            MethodBodyObject methodBodyObject = new MethodBodyObject(methodBody);
            constructorObject.setMethodBody(methodBodyObject);
        }

        for (AnonymousClassDeclarationObject anonymous : constructorObject.getAnonymousClassDeclarations()) {
            anonymous.setClassObject(classObject);
            PsiAnonymousClass anonymousClassDeclaration = anonymous.getAnonymousClassDeclaration();
            int anonymousClassDeclarationStartPosition = anonymousClassDeclaration.getStartOffsetInParent();
            int anonymousClassDeclarationEndPosition = anonymousClassDeclarationStartPosition + anonymousClassDeclaration.getTextLength();
            for (CommentObject comment : constructorObject.commentList) {
                int commentStartPosition = comment.getStartPosition();
                int commentEndPosition = commentStartPosition + comment.getLength();
                if (anonymousClassDeclarationStartPosition <= commentStartPosition && anonymousClassDeclarationEndPosition >= commentEndPosition) {
                    anonymous.addComment(comment);
                }
            }
        }

        if (methodDeclaration.isConstructor()) {
            classObject.addConstructor(constructorObject);
        } else {
            MethodObject methodObject = new MethodObject(methodDeclaration, constructorObject);
            List<PsiAnnotation> extendedModifiers = Arrays.asList(methodDeclaration.getAnnotations());
            for (PsiAnnotation extendedModifier : extendedModifiers) {
                    if (extendedModifier.getQualifiedName().equals("Test")) {
                        methodObject.setTestAnnotation(true);
                        break;
                }
            }
            PsiType returnType = methodDeclaration.getReturnType();
            String qualifiedName = returnType.getCanonicalText();
            TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
            methodObject.setReturnType(typeObject);
            if (methodDeclaration.hasModifier(JvmModifier.ABSTRACT))
                methodObject.setAbstract(true);
            if (methodDeclaration.hasModifier(JvmModifier.STATIC))
                methodObject.setStatic(true);
            if (methodDeclaration.hasModifier(JvmModifier.SYNCHRONIZED))
                methodObject.setSynchronized(true);
            if (methodDeclaration.hasModifier(JvmModifier.NATIVE))
                methodObject.setNative(true);

            classObject.addMethod(methodObject);
            FieldInstructionObject fieldInstruction = methodObject.isGetter();
            if (fieldInstruction != null)
                systemObject.addGetter(methodObject.generateMethodInvocation(), fieldInstruction);
            fieldInstruction = methodObject.isSetter();
            if (fieldInstruction != null)
                systemObject.addSetter(methodObject.generateMethodInvocation(), fieldInstruction);
            fieldInstruction = methodObject.isCollectionAdder();
            if (fieldInstruction != null)
                systemObject.addCollectionAdder(methodObject.generateMethodInvocation(), fieldInstruction);
            MethodInvocationObject methodInvocation = methodObject.isDelegate();
            if (methodInvocation != null)
                systemObject.addDelegate(methodObject.generateMethodInvocation(), methodInvocation);
        }
    }

    /*private List<IMarker> buildProject(IJavaProject iJavaProject, IProgressMonitor pm) {
        ArrayList<IMarker> result = new ArrayList<IMarker>();
        try {
            IProject project = iJavaProject.getProject();
            project.refreshLocal(IResource.DEPTH_INFINITE, pm);
            project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, pm);
            IMarker[] markers = null;
            markers = project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
            for (IMarker marker : markers) {
                Integer severityType = (Integer) marker.getAttribute(IMarker.SEVERITY);
                if (severityType.intValue() == IMarker.SEVERITY_ERROR) {
                    result.add(marker);
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static int getNumberOfCompilationUnits(IJavaProject iJavaProject) {
        int numberOfCompilationUnits = 0;
        try {
            IPackageFragmentRoot[] iPackageFragmentRoots = iJavaProject.getPackageFragmentRoots();
            for (IPackageFragmentRoot iPackageFragmentRoot : iPackageFragmentRoots) {
                IJavaElement[] children = iPackageFragmentRoot.getChildren();
                for (IJavaElement child : children) {
                    if (child.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
                        IPackageFragment iPackageFragment = (IPackageFragment) child;
                        ICompilationUnit[] iCompilationUnits = iPackageFragment.getCompilationUnits();
                        numberOfCompilationUnits += iCompilationUnits.length;
                    }
                }
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
        return numberOfCompilationUnits;
    }*/

/*    public static List<AbstractTypeDeclaration> getRecursivelyInnerTypes(AbstractTypeDeclaration typeDeclaration) {
        List<AbstractTypeDeclaration> innerTypeDeclarations = new ArrayList<AbstractTypeDeclaration>();
        StatementExtractor statementExtractor = new StatementExtractor();
        List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
        for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
            if (bodyDeclaration instanceof MethodDeclaration) {
                MethodDeclaration methodDeclaration = (MethodDeclaration) bodyDeclaration;
                if (methodDeclaration.getBody() != null) {
                    List<Statement> statements = statementExtractor.getTypeDeclarationStatements(methodDeclaration.getBody());
                    for (Statement statement : statements) {
                        TypeDeclarationStatement typeDeclarationStatement = (TypeDeclarationStatement) statement;
                        AbstractTypeDeclaration declaration = typeDeclarationStatement.getDeclaration();
                        if (declaration instanceof TypeDeclaration) {
                            innerTypeDeclarations.add((TypeDeclaration) declaration);
                        }
                    }
                }
            } else if (bodyDeclaration instanceof TypeDeclaration) {
                TypeDeclaration type = (TypeDeclaration) bodyDeclaration;
                innerTypeDeclarations.add(type);
                innerTypeDeclarations.addAll(getRecursivelyInnerTypes(type));
            } else if (bodyDeclaration instanceof EnumDeclaration) {
                EnumDeclaration type = (EnumDeclaration) bodyDeclaration;
                innerTypeDeclarations.add(type);
                innerTypeDeclarations.addAll(getRecursivelyInnerTypes(type));
            }
        }
        return innerTypeDeclarations;
    }*/

/*    private List<ClassObject> parseAST(ICompilationUnit iCompilationUnit) {
        ASTInformationGenerator.setCurrentITypeRoot(iCompilationUnit);
        IFile iFile = (IFile) iCompilationUnit.getResource();
        ASTParser parser = ASTParser.newParser(JLS);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(iCompilationUnit);
        parser.setResolveBindings(true); // we need bindings later on
        CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);

        return parseAST(compilationUnit, iFile);
    }*/

/*    private List<ClassObject> parseAST(CompilationUnit compilationUnit, IFile iFile) {
        ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
        IPath path = compilationUnit.getJavaElement().getPath();
        try {
            bufferManager.connect(path, LocationKind.IFILE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path, LocationKind.IFILE);
        IDocument document = textFileBuffer.getDocument();
        List<Comment> comments = compilationUnit.getCommentList();
        List<ClassObject> classObjects = new ArrayList<ClassObject>();
        List<AbstractTypeDeclaration> topLevelTypeDeclarations = compilationUnit.types();
        for (AbstractTypeDeclaration abstractTypeDeclaration : topLevelTypeDeclarations) {
            if (abstractTypeDeclaration instanceof TypeDeclaration) {
                TypeDeclaration topLevelTypeDeclaration = (TypeDeclaration) abstractTypeDeclaration;
                List<AbstractTypeDeclaration> typeDeclarations = new ArrayList<AbstractTypeDeclaration>();
                typeDeclarations.add(topLevelTypeDeclaration);
                typeDeclarations.addAll(getRecursivelyInnerTypes(topLevelTypeDeclaration));
                for (AbstractTypeDeclaration typeDeclaration : typeDeclarations) {
                    if (typeDeclaration instanceof TypeDeclaration) {
                        final ClassObject classObject = processTypeDeclaration(iFile, document, (TypeDeclaration) typeDeclaration, comments);
                        classObjects.add(classObject);
                    } else if (typeDeclaration instanceof EnumDeclaration) {
                        final ClassObject classObject = processEnumDeclaration(iFile, document, (EnumDeclaration) typeDeclaration, comments);
                        classObjects.add(classObject);
                    }
                }
            } else if (abstractTypeDeclaration instanceof EnumDeclaration) {
                EnumDeclaration enumDeclaration = (EnumDeclaration) abstractTypeDeclaration;
                final ClassObject classObject = processEnumDeclaration(iFile, document, enumDeclaration, comments);
                classObjects.add(classObject);
            }
        }
        return classObjects;
    }*/

/*    private List<CommentObject> processComments(IFile iFile, IDocument iDocument,
                                                AbstractTypeDeclaration typeDeclaration, List<Comment> comments) {
        List<CommentObject> commentList = new ArrayList<CommentObject>();
        int typeDeclarationStartPosition = typeDeclaration.getStartPosition();
        int typeDeclarationEndPosition = typeDeclarationStartPosition + typeDeclaration.getLength();
        for (Comment comment : comments) {
            int commentStartPosition = comment.getStartPosition();
            int commentEndPosition = commentStartPosition + comment.getLength();
            int commentStartLine = 0;
            int commentEndLine = 0;
            String text = null;
            try {
                commentStartLine = iDocument.getLineOfOffset(commentStartPosition);
                commentEndLine = iDocument.getLineOfOffset(commentEndPosition);
                text = iDocument.get(commentStartPosition, comment.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            CommentType type = null;
            if (comment.isLineComment()) {
                type = CommentType.LINE;
            } else if (comment.isBlockComment()) {
                type = CommentType.BLOCK;
            } else if (comment.isDocComment()) {
                type = CommentType.JAVADOC;
            }
            CommentObject commentObject = new CommentObject(text, type, commentStartLine, commentEndLine);
            commentObject.setComment(comment);
            String fileExtension = iFile.getFileExtension() != null ? "." + iFile.getFileExtension() : "";
            if (typeDeclarationStartPosition <= commentStartPosition && typeDeclarationEndPosition >= commentEndPosition) {
                commentList.add(commentObject);
            } else if (iFile.getName().equals(typeDeclaration.getName().getIdentifier() + fileExtension)) {
                commentList.add(commentObject);
            }
        }
        return commentList;
    }*/

/*
    private ClassObject processTypeDeclaration(PsiClass psiClass, PsiFile psiFile) {
        final ClassObject classObject = new ClassObject();
        classObject.setIFile(psiFile);
        //   classObject.addComments(processComments(iFile, document, typeDeclaration, comments));
        //  ITypeBinding typeDeclarationBinding = typeDeclaration.resolveBinding();
*/
/*        if (typeDeclarationBinding.isLocal()) {
            ITypeBinding declaringClass = typeDeclarationBinding.getDeclaringClass();
            String className = declaringClass.getQualifiedName() + "." + typeDeclarationBinding.getName();
            classObject.setName(className);
        } else {
            classObject.setName(typeDeclarationBinding.getQualifiedName());
        }*//*

        //    classObject.setAbstractTypeDeclaration(typeDeclaration);

        if (psiClass.isInterface()) {
            classObject.setInterface(true);
        }

        PsiModifierList modifiers = psiClass.getModifierList();
        if (modifiers == null) return null;
        if (modifiers.hasExplicitModifier(PsiModifier.ABSTRACT)) classObject.setAbstract(true);
        if (modifiers.hasExplicitModifier(PsiModifier.PUBLIC)) classObject.setAccess(Access.PUBLIC);
        else if (modifiers.hasExplicitModifier(PsiModifier.PROTECTED))
            classObject.setAccess(Access.PROTECTED);
        else if (modifiers.hasExplicitModifier(PsiModifier.PRIVATE))
            classObject.setAccess(Access.PRIVATE);
        else
            classObject.setAccess(Access.NONE);

        if (modifiers.hasExplicitModifier(PsiModifier.STATIC))
            classObject.setStatic(true);

        Type superclassType = typeDeclaration.getSuperclassType();
        if (superclassType != null) {
            ITypeBinding binding = superclassType.resolveBinding();
            String qualifiedName = binding.getQualifiedName();
            TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
            classObject.setSuperclass(typeObject);
        }

        List<Type> superInterfaceTypes = typeDeclaration.superInterfaceTypes();
        for (Type interfaceType : superInterfaceTypes) {
            ITypeBinding binding = interfaceType.resolveBinding();
            String qualifiedName = binding.getQualifiedName();
            TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
            classObject.addInterface(typeObject);
        }

        FieldDeclaration[] fieldDeclarations = typeDeclaration.getFields();
        for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
            processFieldDeclaration(classObject, fieldDeclaration);
        }

        MethodDeclaration[] methodDeclarations = typeDeclaration.getMethods();
        for (MethodDeclaration methodDeclaration : methodDeclarations) {
            processMethodDeclaration(classObject, methodDeclaration);
        }
        return classObject;
    }
*/

    /*
        private ClassObject processEnumDeclaration(IFile iFile, IDocument document, EnumDeclaration enumDeclaration, List<Comment> comments) {
            final ClassObject classObject = new ClassObject();
            classObject.setEnum(true);
            classObject.setIFile(iFile);
            classObject.addComments(processComments(iFile, document, enumDeclaration, comments));
            classObject.setName(enumDeclaration.resolveBinding().getQualifiedName());
            classObject.setAbstractTypeDeclaration(enumDeclaration);
    
            int modifiers = enumDeclaration.getModifiers();
            if ((modifiers & Modifier.ABSTRACT) != 0)
                classObject.setAbstract(true);
    
            if ((modifiers & Modifier.PUBLIC) != 0)
                classObject.setAccess(Access.PUBLIC);
            else if ((modifiers & Modifier.PROTECTED) != 0)
                classObject.setAccess(Access.PROTECTED);
            else if ((modifiers & Modifier.PRIVATE) != 0)
                classObject.setAccess(Access.PRIVATE);
            else
                classObject.setAccess(Access.NONE);
    
            if ((modifiers & Modifier.STATIC) != 0)
                classObject.setStatic(true);
    
            List<Type> superInterfaceTypes = enumDeclaration.superInterfaceTypes();
            for (Type interfaceType : superInterfaceTypes) {
                ITypeBinding binding = interfaceType.resolveBinding();
                String qualifiedName = binding.getQualifiedName();
                TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
                classObject.addInterface(typeObject);
            }
    
            List<EnumConstantDeclaration> enumConstantDeclarations = enumDeclaration.enumConstants();
            for (EnumConstantDeclaration enumConstantDeclaration : enumConstantDeclarations) {
                EnumConstantDeclarationObject enumConstantDeclarationObject = new EnumConstantDeclarationObject(enumConstantDeclaration.getName().getIdentifier());
                enumConstantDeclarationObject.setEnumName(classObject.getName());
                enumConstantDeclarationObject.setEnumConstantDeclaration(enumConstantDeclaration);
                List<Expression> arguments = enumConstantDeclaration.arguments();
                for (Expression argument : arguments) {
                    AbstractExpression abstractExpression = new AbstractExpression(argument);
                    enumConstantDeclarationObject.addArgument(abstractExpression);
                }
                classObject.addEnumConstantDeclaration(enumConstantDeclarationObject);
            }
    
            List<BodyDeclaration> bodyDeclarations = enumDeclaration.bodyDeclarations();
            for (BodyDeclaration bodyDeclaration : bodyDeclarations) {
                if (bodyDeclaration instanceof MethodDeclaration) {
                    processMethodDeclaration(classObject, (MethodDeclaration) bodyDeclaration);
                } else if (bodyDeclaration instanceof FieldDeclaration) {
                    processFieldDeclaration(classObject, (FieldDeclaration) bodyDeclaration);
                }
            }
            return classObject;
        }*/
/*
    private void processFieldDeclaration(final ClassObject classObject, FieldDeclaration fieldDeclaration) {
        Type fieldType = fieldDeclaration.getType();
        ITypeBinding binding = fieldType.resolveBinding();
        List<CommentObject> fieldDeclarationComments = new ArrayList<CommentObject>();
        int fieldDeclarationStartPosition = fieldDeclaration.getStartPosition();
        int fieldDeclarationEndPosition = fieldDeclarationStartPosition + fieldDeclaration.getLength();
        for (CommentObject comment : classObject.commentList) {
            int commentStartPosition = comment.getStartPosition();
            int commentEndPosition = commentStartPosition + comment.getLength();
            if (fieldDeclarationStartPosition <= commentStartPosition && fieldDeclarationEndPosition >= commentEndPosition) {
                fieldDeclarationComments.add(comment);
            }
        }
        List<VariableDeclarationFragment> fragments = fieldDeclaration.fragments();
        for (VariableDeclarationFragment fragment : fragments) {
            String qualifiedName = binding.getQualifiedName();
            TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
            typeObject.setArrayDimension(typeObject.getArrayDimension() + fragment.getExtraDimensions());
            FieldObject fieldObject = new FieldObject(typeObject, fragment.getName().getIdentifier());
            fieldObject.setClassName(classObject.getName());
            fieldObject.setVariableDeclarationFragment(fragment);
            fieldObject.addComments(fieldDeclarationComments);

            int fieldModifiers = fieldDeclaration.getModifiers();
            if ((fieldModifiers & Modifier.PUBLIC) != 0)
                fieldObject.setAccess(Access.PUBLIC);
            else if ((fieldModifiers & Modifier.PROTECTED) != 0)
                fieldObject.setAccess(Access.PROTECTED);
            else if ((fieldModifiers & Modifier.PRIVATE) != 0)
                fieldObject.setAccess(Access.PRIVATE);
            else
                fieldObject.setAccess(Access.NONE);

            if ((fieldModifiers & Modifier.STATIC) != 0)
                fieldObject.setStatic(true);

            classObject.addField(fieldObject);
        }
    }
*/
    /*private void processMethodDeclaration(final ClassObject classObject, PsiMethod methodDeclaration) {
        String methodName = methodDeclaration.getName();
        final ConstructorObject constructorObject = new ConstructorObject();
        // constructorObject.setMethodDeclaration(methodDeclaration);
        constructorObject.setName(methodName);
        constructorObject.setClassName(classObject.getName());
        int methodDeclarationStartPosition = methodDeclaration.getTextRange().getStartOffset();
        int methodDeclarationEndPosition = methodDeclarationStartPosition + methodDeclaration.getTextRange().getEndOffset();
        for (CommentObject comment : classObject.commentList) {
            int commentStartPosition = comment.getStartPosition();
            int commentEndPosition = commentStartPosition + comment.getLength();
            if (methodDeclarationStartPosition <= commentStartPosition && methodDeclarationEndPosition >= commentEndPosition) {
                constructorObject.addComment(comment);
            }
        }

*//*        if (methodDeclaration.getJavadoc() != null) {
            Javadoc javaDoc = methodDeclaration.getJavadoc();
            List<TagElement> tags = javaDoc.tags();
            for (TagElement tagElement : tags) {
                String tagName = tagElement.getTagName();
                if (tagName != null && tagName.equals(TagElement.TAG_THROWS)) {
                    List<ASTNode> fragments = tagElement.fragments();
                    for (ASTNode docElement : fragments) {
                        if (docElement instanceof Name) {
                            Name name = (Name) docElement;
                            IBinding binding = name.resolveBinding();
                            if (binding instanceof ITypeBinding) {
                                ITypeBinding typeBinding = (ITypeBinding) binding;
                                constructorObject.addExceptionInJavaDocThrows(typeBinding.getQualifiedName());
                            }
                        }
                    }
                }
            }
        }*//*
        PsiModifierList methodModifiers = methodDeclaration.getModifierList();
        if (methodModifiers.hasExplicitModifier(PsiModifier.PUBLIC))
            constructorObject.setAccess(Access.PUBLIC);
        else if (methodModifiers.hasExplicitModifier(PsiModifier.PROTECTED))
            constructorObject.setAccess(Access.PROTECTED);
        else if (methodModifiers.hasExplicitModifier(PsiModifier.PRIVATE))
            constructorObject.setAccess(Access.PRIVATE);
        else
            constructorObject.setAccess(Access.NONE);

        PsiParameterList parameters = methodDeclaration.getParameterList();
        for (PsiParameter parameter : parameters.getParameters()) {
            PsiType parameterType = parameter.getType();
            PsiElement binding = parameterType;
            String qualifiedName = binding.getQualifiedName();
            TypeObject typeObject = TypeObject.extractTypeObject(qualifiedName);
            typeObject.setArrayDimension(typeObject.getArrayDimension() + parameter.getExtraDimensions());
            if (parameter.isVarargs()) {
                typeObject.setArrayDimension(1);
            }
            ParameterObject parameterObject = new ParameterObject(typeObject, parameter.getName().getIdentifier(), parameter.isVarargs());
            parameterObject.setSingleVariableDeclaration(parameter);
            constructorObject.addParameter(parameterObject);
        }

        PsiCodeBlock methodBody = methodDeclaration.getBody();
        if (methodBody != null) {
            MethodBodyObject methodBodyObject = new MethodBodyObject(methodBody);
            constructorObject.setMethodBody(methodBodyObject);
        }

        for (AnonymousClassDeclarationObject anonymous : constructorObject.getAnonymousClassDeclarations()) {
            anonymous.setClassObject(classObject);
            PsiAnonymousClass anonymousClassDeclaration = anonymous.getAnonymousClassDeclaration();
            int anonymousClassDeclarationStartPosition = anonymousClassDeclaration.getTextRange().getStartOffset();
            int anonymousClassDeclarationEndPosition = anonymousClassDeclarationStartPosition + anonymousClassDeclaration.getTextRange().getLength();
            for (CommentObject comment : constructorObject.commentList) {
                int commentStartPosition = comment.getStartPosition();
                int commentEndPosition = commentStartPosition + comment.getLength();
                if (anonymousClassDeclarationStartPosition <= commentStartPosition && anonymousClassDeclarationEndPosition >= commentEndPosition) {
                    anonymous.addComment(comment);
                }
            }
        }

        if (methodDeclaration.isConstructor()) {
            classObject.addConstructor(constructorObject);
        } else {
            MethodObject methodObject = new MethodObject(constructorObject);
*//*            List<IExtendedModifier> extendedModifiers = methodDeclaration.has;
            for (IExtendedModifier extendedModifier : extendedModifiers) {
                if (extendedModifier.isAnnotation()) {
                    Annotation annotation = (Annotation) extendedModifier;
                    if (annotation.getTypeName().getFullyQualifiedName().equals("Test")) {
                        methodObject.setTestAnnotation(true);
                        break;
                    }
                }*//*
            // }
*//*            Type returnType = methodDeclaration.getReturnType2();
            ITypeBinding binding = returnType.resolveBinding();
            String qualifiedName = binding.getQualifiedName();
            *//*
            TypeObject typeObject = TypeObject.extractTypeObject(methodDeclaration.getReturnTypeElement().getText());
            methodObject.setReturnType(typeObject);

            if (methodModifiers.hasExplicitModifier(PsiModifier.ABSTRACT))
                methodObject.setAbstract(true);
            if (methodModifiers.hasExplicitModifier(PsiModifier.STATIC))
                methodObject.setStatic(true);
            if (methodModifiers.hasExplicitModifier(PsiModifier.SYNCHRONIZED))
                methodObject.setSynchronized(true);
            if (methodModifiers.hasExplicitModifier(PsiModifier.NATIVE))
                methodObject.setNative(true);

            classObject.addMethod(methodObject);
            FieldInstructionObject fieldInstruction = methodObject.isGetter();
            if (fieldInstruction != null)
                systemObject.addGetter(methodObject.generateMethodInvocation(), fieldInstruction);
            fieldInstruction = methodObject.isSetter();
            if (fieldInstruction != null)
                systemObject.addSetter(methodObject.generateMethodInvocation(), fieldInstruction);
            fieldInstruction = methodObject.isCollectionAdder();
            if (fieldInstruction != null)
                systemObject.addCollectionAdder(methodObject.generateMethodInvocation(), fieldInstruction);
            MethodInvocationObject methodInvocation = methodObject.isDelegate();
            if (methodInvocation != null)
                systemObject.addDelegate(methodObject.generateMethodInvocation(), methodInvocation);
        }
    }*/

    public static SystemObject getSystemObject() {
        return systemObject;
    }

    public static ProjectInfo getExaminedProject() {
        return examinedProject;
    }

/*    public static AST getAST() {
        if (systemObject.getClassNumber() > 0) {
            return systemObject.getClassObject(0).getAbstractTypeDeclaration().getAST();
        }
        return null;
    }*/
}