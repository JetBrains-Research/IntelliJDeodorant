package core.distance;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.*;
import core.ast.ClassObject;
import core.ast.FieldInstructionObject;
import core.ast.association.Association;
import core.ast.ASTReader;
import core.ast.MethodInvocationObject;
import core.ast.MethodObject;
import core.ast.ParameterObject;
import org.mozilla.javascript.JavaScriptException;
import utils.IntelliJDeodorantBundle;
import core.ast.util.math.Cluster;
import core.ast.util.math.Clustering;

import java.util.*;

public class DistanceMatrix {
    private final Map<String, Integer> entityIndexMap;
    private final Map<String, Integer> classIndexMap;
    private final List<Entity> entityList;
    private final List<MyClass> classList;
    //holds the entity set of each entity
    private final Map<String, Set<String>> entityMap;
    //holds the entity set of each class
    private final Map<String, Set<String>> classMap;
    private final MySystem system;
    private final String IDENTIFICATION_INDICATOR_TEXT_KEY = "feature.envy.identification.indicator";
    private final int maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate = 2;
    private static final int maximumNumberOfSourceClassMembersAccessedByExtractClassCandidate = 2;

    public DistanceMatrix(MySystem system) {
        this.system = system;
        entityIndexMap = new LinkedHashMap<>();
        classIndexMap = new LinkedHashMap<>();
        entityList = new ArrayList<>();
        classList = new ArrayList<>();
        entityMap = new LinkedHashMap<>();
        classMap = new LinkedHashMap<>();
        generateDistances();
    }

    private void generateDistances() {
        Iterator<MyClass> classIt = system.getClassIterator();
        while (classIt.hasNext()) {
            MyClass myClass = classIt.next();
            ListIterator<MyAttribute> attributeIterator = myClass.getAttributeIterator();
            while (attributeIterator.hasNext()) {
                MyAttribute attribute = attributeIterator.next();
                if (!attribute.isReference()) {
                    entityList.add(attribute);
                    entityMap.put(attribute.toString(), attribute.getEntitySet());
                }
            }
            ListIterator<MyMethod> methodIterator = myClass.getMethodIterator();
            while (methodIterator.hasNext()) {
                MyMethod method = methodIterator.next();
                entityList.add(method);
                entityMap.put(method.toString(), method.getEntitySet());
            }
            classList.add(myClass);
            classMap.put(myClass.getName(), myClass.getEntitySet());
        }

        String[] entityNames = new String[entityList.size()];
        String[] classNames = new String[classList.size()];

        int i = 0;
        for (Entity entity : entityList) {
            entityNames[i] = entity.toString();
            entityIndexMap.put(entityNames[i], i);
            int j = 0;
            for (MyClass myClass : classList) {
                classNames[j] = myClass.getName();
                if (!classIndexMap.containsKey(classNames[j]))
                    classIndexMap.put(classNames[j], j);
                j++;
            }
            i++;
        }
    }

    private List<MoveMethodCandidateRefactoring> identifyConceptualBindings(MyMethod method, Set<String> targetClasses) {
        List<MoveMethodCandidateRefactoring> candidateRefactoringList = new ArrayList<>();
        MethodObject methodObject = method.getMethodObject();
        String sourceClass = method.getClassOrigin();
        for (String targetClass : targetClasses) {
            if (!targetClass.equals(sourceClass)) {
                ClassObject targetClassObject = system.getClass(targetClass).getClassObject();
                ListIterator<ParameterObject> parameterIterator = methodObject.getParameterListIterator();
                while (parameterIterator.hasNext()) {
                    ParameterObject parameter = parameterIterator.next();
                    Association association = system.containsAssociationWithMultiplicityBetweenClasses(targetClass, parameter.getType().getClassType());
                    if (association != null) {
                        List<MethodInvocationObject> methodInvocations = methodObject.getMethodInvocations();
                        for (MethodInvocationObject methodInvocation : methodInvocations) {
                            if (methodInvocation.getOriginClassName().equals(targetClass)) {
                                PsiMethodCallExpression invocation = methodInvocation.getMethodInvocation();
                                boolean parameterIsPassedAsArgument = false;
                                PsiExpression[] invocationArguments = invocation.getArgumentList().getExpressions();
                                for (PsiExpression expression : invocationArguments) {
                                    if (expression instanceof PsiReferenceExpression) {
                                        PsiReferenceExpression argumentName = (PsiReferenceExpression) expression;
                                        if (parameter.getSingleVariableDeclaration().equals(argumentName.resolve()))
                                            parameterIsPassedAsArgument = true;
                                    }
                                }
                                if (parameterIsPassedAsArgument) {
                                    MethodObject invokedMethod = targetClassObject.getMethod(methodInvocation);
                                    List<FieldInstructionObject> fieldInstructions = invokedMethod.getFieldInstructions();
                                    boolean containerFieldIsAccessed = false;
                                    for (FieldInstructionObject fieldInstruction : fieldInstructions) {
                                        if (association.getFieldObject().equals(fieldInstruction)) {
                                            containerFieldIsAccessed = true;
                                            break;
                                        }
                                    }
                                    if (containerFieldIsAccessed) {
                                        MyClass mySourceClass = classList.get(classIndexMap.get(sourceClass));
                                        MyClass myTargetClass = classList.get(classIndexMap.get(targetClass));
                                        MoveMethodCandidateRefactoring candidate = new MoveMethodCandidateRefactoring(system, mySourceClass, myTargetClass, method);
                                        Map<PsiMethodCallExpression, PsiMethod> additionalMethodsToBeMoved = candidate.getAdditionalMethodsToBeMoved();
                                        Collection<PsiMethod> values = additionalMethodsToBeMoved.values();
                                        Set<String> methodEntitySet = entityMap.get(method.toString());
                                        Set<String> sourceClassEntitySet = classMap.get(sourceClass);
                                        Set<String> targetClassEntitySet = classMap.get(targetClass);
                                        Set<String> intersectionWithSourceClass = DistanceCalculator.intersection(methodEntitySet, sourceClassEntitySet);
                                        Set<String> intersectionWithTargetClass = DistanceCalculator.intersection(methodEntitySet, targetClassEntitySet);
                                        Set<String> entitiesToRemoveFromIntersectionWithSourceClass = new LinkedHashSet<>();
                                        if (!values.isEmpty()) {
                                            for (String s : intersectionWithSourceClass) {
                                                int entityPosition = entityIndexMap.get(s);
                                                Entity e = entityList.get(entityPosition);
                                                if (e instanceof MyMethod) {
                                                    MyMethod myInvokedMethod = (MyMethod) e;
                                                    if (values.contains(myInvokedMethod.getMethodObject().getMethodDeclaration())) {
                                                        entitiesToRemoveFromIntersectionWithSourceClass.add(s);
                                                    }
                                                }
                                            }
                                            intersectionWithSourceClass.removeAll(entitiesToRemoveFromIntersectionWithSourceClass);
                                        }
                                        if (intersectionWithTargetClass.size() >= intersectionWithSourceClass.size()) {
                                            if (candidate.isApplicable()) {
                                                int sourceClassDependencies = candidate.getDistinctSourceDependencies();
                                                int targetClassDependencies = candidate.getDistinctTargetDependencies();
                                                if (sourceClassDependencies <= maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate 
                                                        && sourceClassDependencies < targetClassDependencies) {
                                                    candidateRefactoringList.add(candidate);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return candidateRefactoringList;
    }

    private boolean targetClassInheritedByAnotherCandidateTargetClass(String targetClass, Set<String> candidateTargetClasses) {
        for (String candidateTargetClass : candidateTargetClasses) {
            if (!candidateTargetClass.equals(targetClass)) {
                MyClass currentSuperclass = classList.get(classIndexMap.get(candidateTargetClass));
                String superclass;
                while ((superclass = currentSuperclass.getSuperclass()) != null) {
                    if (superclass.equals(targetClass))
                        return true;
                    currentSuperclass = classList.get(classIndexMap.get(superclass));
                }
            }
        }
        return false;
    }

    public List<MoveMethodCandidateRefactoring> getMoveMethodCandidateRefactoringsByAccess(Set<String> classNamesToBeExamined, ProgressIndicator indicator) {
        List<MoveMethodCandidateRefactoring> candidateRefactoringList = new ArrayList<>();
        indicator.setText(IntelliJDeodorantBundle.message(IDENTIFICATION_INDICATOR_TEXT_KEY));
        indicator.setFraction(0.0);
        int entityCount = entityList.size();
        int processedEntities = 0;
        for (Entity entity : entityList) {
            processedEntities += 1;
            if (entity instanceof MyMethod) {
                String sourceClass = entity.getClassOrigin();
                if (classNamesToBeExamined.contains(sourceClass)) {
                    MyMethod method = (MyMethod) entity;
                    Set<String> entitySetI = entityMap.get(entity.toString());
                    Map<String, ArrayList<String>> accessMap = computeAccessMap(entitySetI);
                    List<MoveMethodCandidateRefactoring> conceptuallyBoundRefactorings = identifyConceptualBindings(method, accessMap.keySet());
                    if (!conceptuallyBoundRefactorings.isEmpty()) {
                        candidateRefactoringList.addAll(conceptuallyBoundRefactorings);
                    } else {
                        TreeMap<Integer, ArrayList<String>> sortedByAccessMap = new TreeMap<>();
                        for (String targetClass : accessMap.keySet()) {
                            int numberOfAccessedEntities = accessMap.get(targetClass).size();
                            if (sortedByAccessMap.containsKey(numberOfAccessedEntities)) {
                                ArrayList<String> list = sortedByAccessMap.get(numberOfAccessedEntities);
                                list.add(targetClass);
                            } else {
                                ArrayList<String> list = new ArrayList<>();
                                list.add(targetClass);
                                sortedByAccessMap.put(numberOfAccessedEntities, list);
                            }
                        }

                        boolean candidateFound = false;
                        boolean sourceClassIsTarget = false;
                        while (!candidateFound && !sourceClassIsTarget && !sortedByAccessMap.isEmpty()) {
                            ArrayList<String> targetClasses = sortedByAccessMap.get(sortedByAccessMap.lastKey());
                            for (String targetClass : targetClasses) {
                                if (sourceClass.equals(targetClass)) {
                                    sourceClassIsTarget = true;
                                } else {
                                    MyClass mySourceClass = classList.get(classIndexMap.get(sourceClass));
                                    MyClass myTargetClass = classList.get(classIndexMap.get(targetClass));
                                    MoveMethodCandidateRefactoring candidate = new MoveMethodCandidateRefactoring(system, mySourceClass, myTargetClass, method);
                                    Map<PsiMethodCallExpression, PsiMethod> additionalMethodsToBeMoved = candidate.getAdditionalMethodsToBeMoved();
                                    Collection<PsiMethod> values = additionalMethodsToBeMoved.values();
                                    Set<String> methodEntitySet = entityMap.get(method.toString());
                                    Set<String> sourceClassEntitySet = classMap.get(sourceClass);
                                    Set<String> targetClassEntitySet = classMap.get(targetClass);
                                    Set<String> intersectionWithSourceClass = DistanceCalculator.intersection(methodEntitySet, sourceClassEntitySet);
                                    Set<String> intersectionWithTargetClass = DistanceCalculator.intersection(methodEntitySet, targetClassEntitySet);
                                    Set<String> entitiesToRemoveFromIntersectionWithSourceClass = new LinkedHashSet<>();
                                    if (!values.isEmpty()) {
                                        for (String s : intersectionWithSourceClass) {
                                            int entityPosition = entityIndexMap.get(s);
                                            Entity e = entityList.get(entityPosition);
                                            if (e instanceof MyMethod) {
                                                MyMethod invokedMethod = (MyMethod) e;
                                                if (values.contains(invokedMethod.getMethodObject().getMethodDeclaration())) {
                                                    entitiesToRemoveFromIntersectionWithSourceClass.add(s);
                                                }
                                            }
                                        }
                                        intersectionWithSourceClass.removeAll(entitiesToRemoveFromIntersectionWithSourceClass);
                                    }
                                    if (intersectionWithTargetClass.size() >= intersectionWithSourceClass.size()) {
                                        if (candidate.isApplicable() && !targetClassInheritedByAnotherCandidateTargetClass(targetClass, accessMap.keySet())) {
                                            int sourceClassDependencies = candidate.getDistinctSourceDependencies();
                                            int targetClassDependencies = candidate.getDistinctTargetDependencies();
                                            if (sourceClassDependencies <= maximumNumberOfSourceClassMembersAccessedByMoveMethodCandidate
                                                    && sourceClassDependencies < targetClassDependencies) {
                                                candidateRefactoringList.add(candidate);
                                            }
                                            candidateFound = true;
                                        }
                                    }
                                }
                            }
                            sortedByAccessMap.remove(sortedByAccessMap.lastKey());
                        }
                    }
                }
            }
            indicator.setFraction((double) processedEntities * 100 / entityCount);
        }
        indicator.setFraction(1.0);
        return candidateRefactoringList;
    }

    private Map<String, ArrayList<String>> computeAccessMap(Set<String> entitySetI) {
        Map<String, ArrayList<String>> accessMap = new LinkedHashMap<>();
        for (String e : entitySetI) {
            String[] tokens = e.split("::");
            String classOrigin = tokens[0];
            String entityName = tokens[1];
            if (accessMap.containsKey(classOrigin)) {
                ArrayList<String> list = accessMap.get(classOrigin);
                list.add(entityName);
            } else {
                ArrayList<String> list = new ArrayList<>();
                list.add(entityName);
                if (classMap.containsKey(classOrigin)) accessMap.put(classOrigin, list);
            }
        }

        for (String key1 : accessMap.keySet()) {
            ClassObject classObject = ASTReader.getSystemObject().getClassObject(key1);
            if (classObject != null && classObject.getSuperclass() != null) {
                for (String key2 : accessMap.keySet()) {
                    if (classObject.getSuperclass().getClassType().equals(key2)) {
                        ArrayList<String> list = accessMap.get(key1);
                        list.addAll(accessMap.get(key2));
                    }
                }
            }
        }
        return accessMap;
    }

    public double[][] getJaccardDistanceMatrix(MyClass sourceClass) {
        ArrayList<Entity> entities = new ArrayList<>();
        entities.addAll(sourceClass.getAttributeList());
        entities.addAll(sourceClass.getMethodList());
        double[][] jaccardDistanceMatrix = new double[entities.size()][entities.size()];
        for (int i = 0; i < jaccardDistanceMatrix.length; i++) {
            for (int j = 0; j < jaccardDistanceMatrix.length; j++) {
                if (i != j) {
                    jaccardDistanceMatrix[i][j] = DistanceCalculator.getDistance(entities.get(i).getFullEntitySet(), entities.get(j).getFullEntitySet());
                } else {
                    jaccardDistanceMatrix[i][j] = 0.0;
                }
            }
        }
        return jaccardDistanceMatrix;
    }

    public List<ExtractClassCandidateRefactoring> getExtractClassCandidateRefactorings(Set<String> classNamesToBeExamined, ProgressIndicator indicator) {
        List<ExtractClassCandidateRefactoring> candidateList = new ArrayList<>();
        Iterator<MyClass> classIt = system.getClassIterator();
        ArrayList<MyClass> oldClasses = new ArrayList<>();

        while(classIt.hasNext()) {
            MyClass myClass = classIt.next();
            if(classNamesToBeExamined.contains(myClass.getName())) {
                oldClasses.add(myClass);
            }
        }

        /*
        TODO
        if(indicator != null)
            indicator.beginTask("Identification of Extract Class refactoring opportunities", oldClasses.size());
         */
        for(MyClass sourceClass : oldClasses) {
            /*
            if(monitor != null && monitor.isCanceled())
                throw new OperationCanceledException();
                TODO
            */
            if (!sourceClass.getMethodList().isEmpty() && !sourceClass.getAttributeList().isEmpty()) {
                double[][] distanceMatrix = getJaccardDistanceMatrix(sourceClass);
                Clustering clustering = Clustering.getInstance(0, distanceMatrix);
                ArrayList<Entity> entities = new ArrayList<>();
                entities.addAll(sourceClass.getAttributeList());
                entities.addAll(sourceClass.getMethodList());
                HashSet<Cluster> clusters = clustering.clustering(entities);
                for (Cluster cluster : clusters) {
                    ExtractClassCandidateRefactoring candidate = new ExtractClassCandidateRefactoring(system, sourceClass, cluster.getEntities());
                    if (candidate.isApplicable()) {
                        int sourceClassDependencies = candidate.getDistinctSourceDependencies();
                        int extractedClassDependencies = candidate.getDistinctTargetDependencies();
                        if(sourceClassDependencies <= maximumNumberOfSourceClassMembersAccessedByExtractClassCandidate &&
                                sourceClassDependencies < extractedClassDependencies) {
                            candidateList.add(candidate);
                        }
                    }
                }
                // Clustering End
            }
            /*
            if(monitor != null)
                monitor.worked(1);
                TODO
             */
        }
        /*
        if(monitor != null)
            monitor.done();
            TODO
         */
        return candidateList;
    }
}
