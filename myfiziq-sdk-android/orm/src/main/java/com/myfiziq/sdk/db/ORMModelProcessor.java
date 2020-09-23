package com.myfiziq.sdk.db;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**
 * @hide
 */

@SupportedAnnotationTypes("com.myfiziq.sdk.db.Persistent")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ORMModelProcessor extends AbstractProcessor
{
    public static final String SUFFIX = "_ORM";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_generateReadFromCursor = false;
    private static final boolean DEBUG_generateGetContentValues = false;

    private Types mTypeUtils;
    private Elements mElementUtils;
    private Filer mFiler;
    private Messager mMessager;

    ClassName mOrm = ClassName.get("com.myfiziq.sdk.db", "Orm");
    ClassName mModel = ClassName.get("com.myfiziq.sdk.db", "Model");
    ClassName mDateTime = ClassName.get("org.joda.time", "DateTime");
    ClassName mTimeUtils = ClassName.get("com.myfiziq.sdk.utils", "TimeUtil");
    ClassName mArrayList = ClassName.get("java.util", "ArrayList");
    ClassName mString = ClassName.get("java.lang", "String");
    ClassName mTextUtils = ClassName.get("android.text", "TextUtils");
    ClassName mOgnDbCache = ClassName.get("com.myfiziq.sdk.db", "ORMDbCache");
    ClassName mOGNTable = ClassName.get("com.myfiziq.sdk.db", "ORMTable");
    ClassName mModelBasic = ClassName.get("com.myfiziq.sdk.db", "ModelBasicType");
    ClassName mContentValues = ClassName.get("android.content", "ContentValues");
    ClassName mHashSet = ClassName.get("java.util", "HashSet");
    ClassName mIterator = ClassName.get("java.util", "Iterator");
    ClassName mCached = ClassName.get("com.myfiziq.sdk.db", "Cached");
    ClassName mGson = ClassName.get("com.google.gson", "Gson");

    TypeElement mModelBasicType;
    TypeElement mModelType;
    TypeElement ENUM;
    //TypeElement mDateTimeType;
    TypeElement COLLECTION;
    TypeElement MAP;
    TypeElement HASHMAP;
    TypeElement VOID;
    WildcardType WILDCARD_TYPE_NULL;

    Map<String, DeclaredType> mCachedParentTypes = new HashMap<>();

    static class ModelSet
    {
        public TypeElement mRoot;
        public List<Element> mElements = new ArrayList<>();

        public ModelSet(TypeElement root)
        {
            mRoot = root;
        }

        public ModelSet(TypeElement root, Element child)
        {
            mRoot = root;
            mElements.add(child);
        }

        public void add(Element child)
        {
            if (!mElements.contains(child))
                mElements.add(child);
        }
    }

    HashMap<String, ModelSet> mElementMap = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment env)
    {
        super.init(env);
        mTypeUtils = processingEnv.getTypeUtils();
        mElementUtils = processingEnv.getElementUtils();
        mFiler = processingEnv.getFiler();
        mMessager = processingEnv.getMessager();
        //mDateTimeType = mElementUtils.getTypeElement("org.joda.time.DateTime");
        mModelType = mElementUtils.getTypeElement("com.myfiziq.sdk.db.Model");
        mModelBasicType = mElementUtils.getTypeElement("com.myfiziq.sdk.db.ModelBasicType");
        ENUM = mElementUtils.getTypeElement("java.lang.Enum");
        COLLECTION = mElementUtils.getTypeElement("java.util.Collection");
        MAP = mElementUtils.getTypeElement("java.util.Map");
        HASHMAP = mElementUtils.getTypeElement("java.util.HashMap");
        VOID = mElementUtils.getTypeElement("java.lang.Void");
        WILDCARD_TYPE_NULL = mTypeUtils.getWildcardType(null, null);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env)
    {
        Collection<? extends Element> annotatedElements =
                env.getElementsAnnotatedWith(Persistent.class);

        for (Element element : annotatedElements)
        {
            Element rootElement = element.getEnclosingElement();

            if (rootElement instanceof TypeElement)
            {
                TypeElement rootTypeElem = (TypeElement) rootElement;
                String rootElemName = rootTypeElem.getQualifiedName().toString();
                handleSetElement(rootElemName, element);
            }
        }

        for (final ModelSet set : mElementMap.values())
        {
            TypeElement rootElem = set.mRoot;
            for (TypeMirror type : mTypeUtils.directSupertypes(rootElem.asType()))
            {
                handleSuperSetElement(set, ((DeclaredType) type).asElement());
            }
        }

        for (final ModelSet set : mElementMap.values())
        {
            Collections.sort(set.mElements, new Comparator<Element>()
            {
                @Override
                public int compare(Element lhs, Element rhs)
                {
                    String lhsName = lhs.getSimpleName().toString();
                    String rhsName = rhs.getSimpleName().toString();
                    Persistent lhsAno = lhs.getAnnotation(Persistent.class);
                    Persistent rhsAno = rhs.getAnnotation(Persistent.class);

                    if (lhsName.contentEquals(rhsName))
                        return 0;

                    if (lhsName.contentEquals("id"))
                        return -1;

                    if (rhsName.contentEquals("id"))
                        return 1;

                    if (lhsAno.order() != rhsAno.order())
                    {
                        return lhsAno.order() - rhsAno.order();
                    }

                    return lhsName.compareTo(rhsName);
                }
            });

            processSet(set);
        }
//        List<TypeElement> types =
//                new ImmutableList.Builder<TypeElement>()
//                        .addAll(ElementFilter.typesIn(annotatedElements))
//                        .build();
//
//        for (TypeElement type : types)
//        {
//            processType(type);
//        }

        return true;
    }

    private void handleSetElement(String elementName, Element element)
    {
        if (null != element)
        {
            Element rootElement = element.getEnclosingElement();

            if (null != rootElement && rootElement instanceof TypeElement)
            {
                if (DEBUG)
                    mMessager.printMessage(Diagnostic.Kind.NOTE, String.format("ORMModelProcessor handleSetElement: (%s) %s - %s", elementName, rootElement.getSimpleName(), element.getSimpleName()));

                TypeElement rootTypeElem = (TypeElement) rootElement;
                ModelSet set = mElementMap.get(elementName);
                if (null != set)
                {
                    set.add(element);
                }
                else
                {
                    set = new ModelSet(rootTypeElem, element);
                    mElementMap.put(elementName, set);
                }
            }
        }
    }

    private void handleSuperSetElement(ModelSet set, Element element)
    {
        String elementName = set.mRoot.getQualifiedName().toString();

        if (null != element && element instanceof TypeElement)
        {
            TypeElement rootTypeElem = (TypeElement) element;
            String superElementName = rootTypeElem.getQualifiedName().toString();

            if (DEBUG)
                mMessager.printMessage(Diagnostic.Kind.NOTE, String.format("ORMModelProcessor handleSuperSetElement super: (%s) %s", elementName, superElementName));

            ModelSet superSet = mElementMap.get(superElementName);
            if (null != superSet)
            {
                for (Element e : superSet.mElements)
                {
                    handleSetElement(elementName, e);
                }

                for (TypeMirror type : mTypeUtils.directSupertypes(superSet.mRoot.asType()))
                {
                    handleSuperSetElement(set, ((DeclaredType) type).asElement());
                }
            }
        }
    }

    private void processSet(ModelSet set)
    {
        if (DEBUG)
            mMessager.printMessage(Diagnostic.Kind.NOTE, "ORMModelProcessor processType:" + set.mRoot.getQualifiedName());
        TypeElement superClassName = mElementUtils.getTypeElement(set.mRoot.getQualifiedName());
        String factoryClassName = superClassName.getSimpleName() + SUFFIX;
        JavaFile source = generateClass(set, factoryClassName);
        writeSourceFile(factoryClassName, source);
    }

    private JavaFile generateClass(ModelSet set, String className)
    {
        TypeElement type = set.mRoot;

        if (type == null)
        {
            //mErrorReporter.abortWithError("generateClass was invoked with null type", type);
        }
        if (className == null)
        {
            //mErrorReporter.abortWithError("generateClass was invoked with null class name", type);
        }

        String classToExtend = set.mRoot.getQualifiedName().toString();
        String pkg = packageNameOf(type);
        TypeName classTypeName = ClassName.get(pkg, className);

        TypeSpec.Builder subClass = TypeSpec.classBuilder(className)
                // Class must be always final
                //.addModifiers(Modifier.FINAL);
                // extends from original abstract class
                .superclass(ClassName.get(pkg, classToExtend))
                // Add the DEFAULT constructor
                //.addMethod(generateConstructor(properties))
                // overrides describeContents()
                // import com.myfiziq.sdk.db.Orm;
                .addMethod(generateReadFromCursor(set))
                .addMethod(generateCopy(set))
                .addMethod(generateSave(set))
                .addMethod(generateSaveThis(set))
                .addMethod(generateSaveChildren(set))
                .addMethod(generateGetContentValues(set));
        // static final CREATOR
        //.addField(generateCreator(processingEnv, properties, classTypeName, typeAdapters))
        // overrides writeToParcel()
        //.addMethod(generateWriteToParcel(version, processingEnv, properties, typeAdapters)); // generate writeToParcel()
        if (DEBUG)
            mMessager.printMessage(Diagnostic.Kind.NOTE, String.format("ORMModelProcessor generateClass - next"));

        JavaFile javaFile = JavaFile.builder(pkg, subClass.build()).build();
        return javaFile;
    }

    static String packageNameOf(TypeElement type)
    {
        while (true)
        {
            Element enclosing = type.getEnclosingElement();
            if (enclosing instanceof PackageElement)
            {
                return ((PackageElement) enclosing).getQualifiedName().toString();
            }
            type = (TypeElement) enclosing;
        }
    }

    private void writeSourceFile(
            String className,
            JavaFile file)
    {
        try
        {
            //mMessager.printMessage(Diagnostic.Kind.NOTE, "ORMModelProcessor writeSourceFile:" + className);

            file.writeTo(mFiler);
        }
        catch (IOException e)
        {// silent}
        }
    }

//    private void writeSourceFile(
//            String className,
//            String text,
//            TypeElement originatingType)
//    {
//        try
//        {
//            mMessager.printMessage(Diagnostic.Kind.NOTE, "ORMModelProcessor writeSourceFile:" + className);
//
//            JavaFileObject sourceFile =
//                    processingEnv.getFiler().
//                            createSourceFile(className, originatingType);
//            Writer writer = sourceFile.openWriter();
//            try
//            {
//                writer.write(text);
//            }
//            finally
//            {
//                writer.close();
//            }
//        }
//        catch (IOException e)
//        {// silent}
//        }
//    }

    public boolean isA(TypeMirror type, TypeElement typeElement)
    {
        // Have we used this type before?
        DeclaredType parentType = mCachedParentTypes.get(typeElement.getQualifiedName().toString());
        if (parentType == null)
        {
            // How many generic type parameters does this typeElement require?
            int genericsCount = typeElement.getTypeParameters().size();

            // Fill the right number of types with nulls
            TypeMirror[] types = new TypeMirror[genericsCount];
            for (int i = 0; i < genericsCount; i++)
            {
                types[i] = WILDCARD_TYPE_NULL;
            }

            // Locate the correct DeclaredType to match with the type
            parentType = mTypeUtils.getDeclaredType(typeElement, types);

            // Remember this DeclaredType
            mCachedParentTypes.put(typeElement.getQualifiedName().toString(), parentType);
        }

        // Is the given type able to be assigned as the typeElement?
        return mTypeUtils.isAssignable(type, parentType);
    }

    MethodSpec generateReadFromCursor(ModelSet set)
    {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("readFromCursor")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(ClassName.get("android.database", "Cursor"), "cursor").build())
                .addParameter(ParameterSpec.builder(mModel, "parent").build())
                .addParameter(ParameterSpec.builder(int.class, "childDepth").build())
                .addParameter(ParameterSpec.builder(int.class, "childArrayDepth").build())
                .addParameter(ParameterSpec.builder(String[].class, "fieldsToRead").build());

        builder.beginControlFlow("if (!cursor.isClosed())");

        builder.addStatement("childDepth--");
        builder.addStatement("childArrayDepth--");
        builder.addStatement("int cursorColIndex = 0");

        // First index (0) is PK alias (see OGNContentProvider->updateProjectionJoined).
        int index = 1;
        for (Element elem : set.mElements)
        {
            Persistent persistent = elem.getAnnotation(Persistent.class);

            if (DEBUG_generateReadFromCursor)
                mMessager.printMessage(Diagnostic.Kind.NOTE, String.format("generateReadFromCursor (%s) %d %s", set.mRoot.getSimpleName().toString(), index, elem.getSimpleName().toString()));

            TypeMirror type = elem.asType();
            String name = elem.getSimpleName().toString();

            switch (type.getKind())
            {
                case DECLARED:
                {
                    String typeName = type.toString();
                    if (typeName.startsWith(ArrayList.class.getName()))
                    {
                        type = ((DeclaredType) type).getTypeArguments().get(0);
                        if (isA(type, mModelType))
                        {
                            if (DEBUG_generateReadFromCursor)
                                mMessager.printMessage(
                                        Diagnostic.Kind.NOTE,
                                        String.format("ORMModelProcessor generateReadFromCursor (list) typeName:%s type:%s persistent:%s", typeName, type, persistent));

                            String query = persistent.query();

                            if (query.length() > 0)
                            {
                                builder.addStatement("$T $Lquery = $S", mString, name, query);
                                builder.addStatement("$1Lquery = replaceFields($1Lquery)", name);
                                builder.addStatement("$1T $Llist = $2T.dbFromModel($3T.class).getModelList($3T.class, this, $4S, null)", mArrayList, mOGNTable, name, query);
                                builder.addStatement("$1L=$1Llist", name);
                            }
                            else
                            {
                                builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                                builder.beginControlFlow("if (cursorColIndex >= 0)");
                                builder.addStatement("$1T $2Llist = new $1T()", mArrayList, name);
                                builder.addStatement("$1L=$1Llist", name);
                                builder.addStatement("$T $Lindexes = cursor.getString(cursorColIndex)", mString, name);
                                builder.beginControlFlow("if (!$T.isEmpty($Lindexes))", mTextUtils, name);

                                builder.addStatement("$T[] $Ltokens = $Lindexes.split(Character.toString((char) 1))", mString, name, name);
                                builder.beginControlFlow("for (String idStr : $Ltokens)", name);

                                builder.beginControlFlow("if (!$T.isEmpty(idStr))", mTextUtils);
                                builder.addStatement("Model m = null");
                                builder.addStatement("m = $T.getInstance().getModel($T.class, this, idStr, childDepth, childArrayDepth)", mOgnDbCache, type);

                                builder.beginControlFlow("if (null == m)");
                                builder.addStatement("m = $1T.dbFromModel($2T.class).getModel($2T.class, this, String.format(\"%s='%s'\", getIdFieldName($2T.class), idStr), null, childDepth, childArrayDepth)", mOGNTable, type);
                                builder.endControlFlow();

                                builder.beginControlFlow("if (null != m)");
                                builder.addStatement("$1Llist.add(m)", name);
                                builder.endControlFlow();

                                builder.endControlFlow();
                                builder.endControlFlow();

                                builder.endControlFlow();

                                builder.endControlFlow();
                            }
                        }
                    }
                    else if (typeName.contentEquals(String.class.getName()))
                    {
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (cursorColIndex >= 0)");
                        builder.addStatement(elem.getSimpleName() + "=cursor.getString(cursorColIndex)");
                        builder.endControlFlow();
                    }
                    else if (isA(type, mModelBasicType))
                    {
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (cursorColIndex >= 0)");
                        builder.addStatement("$1L = new $2T()", name, elem);
                        builder.addStatement("$1L.readFromCursor(cursor, cursorColIndex, this)", name);
                        builder.endControlFlow();
                    }
                    else if (isA(type, mModelType))
                    {
                        if (DEBUG_generateReadFromCursor)
                            mMessager.printMessage(
                                    Diagnostic.Kind.NOTE,
                                    String.format("ORMModelProcessor generateReadFromCursor (model) type:%s name:%s", typeName, name));
                        builder.beginControlFlow("if ($L && parent instanceof $T)", persistent.fromParent(), elem);
                        builder.addStatement("$L=($T)parent", name, elem);
                        builder.endControlFlow();
                        builder.beginControlFlow("else if (childDepth >= 0)");

                        String query = persistent.query();
                        if (null != query && query.length() > 0)
                        {
                            builder.addStatement("$T $Lquery = $S", mString, name, query);
                            builder.addStatement("$1Lquery = replaceFields($1Lquery)", name);
                            builder.addStatement("$3L = $2T.dbFromModel($3T.class).getModel($3T.class, this, $4S, null, childDepth, childArrayDepth)", mArrayList, mOGNTable, name, query);
                        }
                        else
                        {
                            builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                            builder.beginControlFlow("if (cursorColIndex >= 0)");
                            builder.addStatement("$T $LidStr = cursor.getString(cursorColIndex)", mString, name);
                            builder.beginControlFlow("if (!$T.isEmpty($LidStr))", mTextUtils, name);
                            builder.addStatement("$T $Lmodel = null", elem, name);
                            builder.addStatement("$1Lmodel = $2T.getInstance().getModel($3T.class, this, $1LidStr, childDepth, childArrayDepth)", name, mOgnDbCache, elem);
                            builder.beginControlFlow("if (null == $1Lmodel)", name);
                            builder.addStatement("$1Lmodel = $2T.dbFromModel($3T.class).getModel($3T.class, this, String.format(\"$3T.id='%s'\", $1LidStr), null, childDepth, childArrayDepth)", name, mOGNTable, elem);
                            builder.endControlFlow();
                            builder.addStatement("$1L=$1Lmodel", name);
                            builder.endControlFlow();
                            builder.endControlFlow();
                        }
                        builder.endControlFlow();
                    }
                    else if (isA(type, ENUM))
                    {
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (cursorColIndex >= 0)");
                        builder.addStatement("$T n = cursor.getString(cursorColIndex)", mString);
                        builder.beginControlFlow("if (!$T.isEmpty(n))", mTextUtils);
                        builder.addStatement(elem.getSimpleName() + "=$1T.valueOf(n)", elem);
                        builder.endControlFlow();
                        builder.endControlFlow();
                    }
                    /*
                    else if (isA(type, mDateTimeType))
                    {
                        mMessager.printMessage(Diagnostic.Kind.NOTE, "ORMModelProcessor generateReadFromCursor (DateTime):" + typeName);
                        builder.beginControlFlow("if (cursorColIndex >= 0)");
                        builder.addStatement("$2L = $1T.getDateTime(cursor.getString(cursor.getColumnIndex($2S)))", mTimeUtils, name);
                        builder.endControlFlow();
                    }
                    */
                    else if (typeName.contentEquals(Boolean.class.getName()) || typeName.contentEquals(boolean.class.getName()))
                    {
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (cursorColIndex >= 0)");
                        builder.addStatement(elem.getSimpleName() + "=(cursor.getInt(cursorColIndex)==1)");
                        builder.endControlFlow();
                    }
                    else if (typeName.contentEquals(Integer.class.getName()) || typeName.contentEquals(int.class.getName()))
                    {
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (cursorColIndex >= 0)");
                        builder.addStatement(elem.getSimpleName() + "=cursor.getInt(cursorColIndex)");
                        builder.endControlFlow();
                    }
                    else if (typeName.contentEquals(Long.class.getName()) || typeName.contentEquals(long.class.getName()))
                    {
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (cursorColIndex >= 0)");
                        builder.addStatement(elem.getSimpleName() + "=cursor.getLong(cursorColIndex)");
                        builder.endControlFlow();
                    }
                    else if (typeName.contentEquals(Double.class.getName()) || typeName.contentEquals(double.class.getName()))
                    {
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (cursorColIndex >= 0)");
                        builder.addStatement(elem.getSimpleName() + "=cursor.getDouble(cursorColIndex)");
                        builder.endControlFlow();
                    }
                    else if (typeName.contentEquals(Float.class.getName()) || typeName.contentEquals(float.class.getName()))
                    {
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (cursorColIndex >= 0)");
                        builder.addStatement(elem.getSimpleName() + "=cursor.getFloat(cursorColIndex)");
                        builder.endControlFlow();
                    }
                    else
                    {
                        if (DEBUG_generateReadFromCursor)
                            mMessager.printMessage(Diagnostic.Kind.NOTE, "ORMModelProcessor generateReadFromCursor unhandled:" + typeName);
                    }
                    //DeclaredType dc = mTypeUtils.getDeclaredType((TypeElement) elem);
                }
                break;

                case ARRAY:
                {
                    //if (DEBUG_generateReadFromCursor)
                    //    mMessager.printMessage(Diagnostic.Kind.NOTE, "ORMModelProcessor generateReadFromCursor [array]:" + elem.getSimpleName());

                    ArrayType asArrayType = (ArrayType) elem.asType();
                    switch(asArrayType.getComponentType().getKind())
                    {
                        case BYTE:
                            if (DEBUG_generateReadFromCursor)
                                mMessager.printMessage(Diagnostic.Kind.NOTE, "ORMModelProcessor generateReadFromCursor [byte array]:" + elem.getSimpleName());
                            builder.beginControlFlow("if (cursorColIndex >= 0)");
                            builder.addStatement(elem.getSimpleName() + "=cursor.getBlob(cursorColIndex)");
                            builder.endControlFlow();
                            break;
                    }
                }
                break;

                case BOOLEAN:
                    builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                    builder.beginControlFlow("if (cursorColIndex >= 0)");
                    builder.addStatement(elem.getSimpleName() + "=(cursor.getInt(cursorColIndex)==1)");
                    builder.endControlFlow();
                    break;

                case LONG:
                    builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                    builder.beginControlFlow("if (cursorColIndex >= 0)");
                    builder.addStatement(elem.getSimpleName() + "=cursor.getLong(cursorColIndex)");
                    builder.endControlFlow();
                    break;

                case BYTE:
                case INT:
                    builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                    builder.beginControlFlow("if (cursorColIndex >= 0)");
                    builder.addStatement(elem.getSimpleName() + "=cursor.getInt(cursorColIndex)");
                    builder.endControlFlow();
                    break;

                case DOUBLE:
                    builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                    builder.beginControlFlow("if (cursorColIndex >= 0)");
                    builder.addStatement(elem.getSimpleName() + "=cursor.getDouble(cursorColIndex)");
                    builder.endControlFlow();
                    break;

                case FLOAT:
                    builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                    builder.beginControlFlow("if (cursorColIndex >= 0)");
                    builder.addStatement(elem.getSimpleName() + "=cursor.getFloat(cursorColIndex)");
                    builder.endControlFlow();
                    break;

                default:
                    if (DEBUG_generateReadFromCursor)
                        mMessager.printMessage(Diagnostic.Kind.NOTE, "ORMModelProcessor generateReadFromCursor unhandled:" + type);
                    break;
            }

            index++;
        }
        builder.endControlFlow();

        builder.addStatement("afterReadFromCursor()");

        return builder.build();
    }
//
//    MethodSpec generateGetName(ModelSet set)
//    {
//        MethodSpec.Builder builder = MethodSpec.methodBuilder("getName")
//                .addAnnotation(Override.class)
//                .addModifiers(Modifier.PUBLIC)
//                .addModifiers(Modifier.STATIC)
//                .returns(String.class);
//
//        builder.addStatement("return $S", set.mRoot.getSimpleName().toString());
//        return builder.build();
//    }

    MethodSpec generateGetContentValues(ModelSet set)
    {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("getContentValues")
                .addParameter(ParameterSpec.builder(ClassName.get("android.database", "Cursor"), "cursor").build())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(mContentValues);

        if (DEBUG_generateGetContentValues)
            mMessager.printMessage(Diagnostic.Kind.NOTE, String.format("generateGetContentValues"));

        //TypeMirror model = mElementUtils.getTypeElement("com.myfiziq.sdk.db.Model").asType();
        //TypeMirror modelBasicType = mElementUtils.getTypeElement("com.myfiziq.sdk.db.ModelBasicType").asType();
        //TypeMirror dateTime = mElementUtils.getTypeElement("org.joda.time.DateTime").asType();

        builder.addStatement("ContentValues values = new ContentValues()");
        builder.addStatement("boolean bTryUpdate = (null != cursor && cursor.getCount() > 0)");
        builder.addStatement("int cursorColIndex = 0");

        for (Element elem : set.mElements)
        {
            Persistent persistent = elem.getAnnotation(Persistent.class);
            if (DEBUG_generateGetContentValues)
                mMessager.printMessage(Diagnostic.Kind.NOTE, String.format("generateGetContentValues (%s) %s",
                        set.mRoot.getSimpleName().toString(), elem.getSimpleName().toString()));

            if ((null != persistent) && (!persistent.pk()) && (!persistent.joinedField()) && (persistent.inDb()))
            {
                TypeMirror type = elem.asType();
                String name = elem.getSimpleName().toString();

                switch (type.getKind())
                {
                    case DECLARED:
                    {
                        String typeName = type.toString();

                        if (typeName.startsWith(ArrayList.class.getName()))
                        {
                            type = ((DeclaredType) type).getTypeArguments().get(0);
                            if (mTypeUtils.isAssignable(type, mModelType.asType()))
                            {
                                String query = persistent.query();

                                if (query.length() <= 0)
                                {
                                    builder.addStatement("boolean bChanged$1L = false", name);
                                    builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                                    builder.beginControlFlow("if (bTryUpdate && !cursor.isNull(cursorColIndex))");
                                    builder.addStatement("$1T<$2T> srcids = new $1T<>()", mHashSet, mString);
                                    builder.addStatement("$1T<$2T> dstids = new $1T<>()", mHashSet, mString);
                                    builder.addStatement("$1T<$2T> it", mIterator, mString);
                                    builder.addStatement("$1T indexes = cursor.getString(cursorColIndex)", mString);
                                    builder.beginControlFlow("if (!$T.isEmpty(indexes))", mTextUtils);
                                    builder.addStatement("$1T[] tokens = indexes.split(Character.toString((char) 1))", mString);
                                    builder.beginControlFlow("for ($T idStr : tokens)", mString);
                                    builder.beginControlFlow("if (!$T.isEmpty(idStr))", mTextUtils);
                                    builder.addStatement("srcids.add(idStr)");
                                    builder.endControlFlow();
                                    builder.endControlFlow();
                                    builder.endControlFlow();
                                    builder.beginControlFlow("for ($1T m : $2L)", mModel, name);
                                    builder.addStatement("dstids.add(m.getId())");
                                    builder.endControlFlow();
                                    builder.addStatement("int srcSize = srcids.size()");
                                    builder.addStatement("int dstSize = dstids.size()");
                                    builder.beginControlFlow("if (srcSize >= dstSize)");
                                    builder.addStatement("it = srcids.iterator()");
                                    builder.beginControlFlow("while (it.hasNext())");
                                    builder.addStatement("$T id = it.next()", mString);
                                    builder.beginControlFlow("if (!dstids.remove(id))");
                                    builder.addStatement("bChanged$1L = true", name);
                                    builder.addStatement("break");
                                    builder.endControlFlow();
                                    builder.endControlFlow();
                                    builder.endControlFlow();
                                    builder.beginControlFlow("else");
                                    builder.addStatement("it = dstids.iterator()");
                                    builder.beginControlFlow("while (it.hasNext())");
                                    builder.addStatement("$T id = it.next()", mString);
                                    builder.beginControlFlow("if (!srcids.remove(id))");
                                    builder.addStatement("bChanged$1L = true", name);
                                    builder.addStatement("break");
                                    builder.endControlFlow();
                                    builder.endControlFlow();
                                    builder.endControlFlow();
                                    builder.endControlFlow();
                                    builder.beginControlFlow("else");
                                    builder.addStatement("bChanged$1L = true", name);
                                    builder.endControlFlow();
                                    builder.beginControlFlow("if (bChanged$1L)", name);
                                    builder.beginControlFlow("if ((($T)$L).size()>0)", mArrayList, name);
                                    builder.beginControlFlow("for (Object o : ($T)$L)", mArrayList, name);
                                    builder.beginControlFlow("if (null != o)");
                                    builder.addStatement("$1T m = ($1T) o", mModel);
                                    builder.addStatement("$1T val = ($1T) values.get($2S)", mString, name);
                                    builder.beginControlFlow("if ($T.isEmpty(val))", mTextUtils);
                                    builder.addStatement("val = Character.toString((char) 1) + m.getId() + Character.toString((char) 1)");
                                    builder.endControlFlow();
                                    builder.beginControlFlow("else");
                                    builder.addStatement("val += m.getId() + Character.toString((char) 1)");
                                    builder.endControlFlow();
                                    builder.addStatement("values.put($S, val)", name);
                                    builder.endControlFlow();
                                    builder.endControlFlow();
                                    builder.endControlFlow();
                                    builder.beginControlFlow("else");
                                    builder.addStatement("values.put($S, \"\")", name);
                                    builder.endControlFlow();
                                    builder.endControlFlow();
                                }
                            }
                        }
                        else if (typeName.contentEquals(String.class.getName()))
                        {
                            builder.beginControlFlow("if (null != $L)", name);
                            builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                            builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || !cursor.getString(cursorColIndex).contentEquals((String)$1L))", name);
                            builder.addStatement("values.put($1S, (String) $1L) ", name);
                            builder.endControlFlow();
                            builder.endControlFlow();
                        }
                        /*
                        else if (mTypeUtils.isAssignable(type, dateTime))
                        {
                            builder.beginControlFlow("if (null != $L)", name);
                            builder.addStatement("$1T $3Ldt = $2T.getStrDateTime($3L)", mString, mTimeUtils, name);
                            builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursor.getColumnIndex($1S)) || !cursor.getString(cursor.getColumnIndex($1S)).contentEquals($1Ldt))", name);
                            builder.addStatement("values.put($1S, $1Ldt) ", name);
                            builder.endControlFlow();
                            builder.endControlFlow();
                            builder.beginControlFlow("else if (!bTryUpdate || cursor.isNull(cursor.getColumnIndex($1S)) || !cursor.getString(cursor.getColumnIndex($1S)).contentEquals(\"\"))", name);
                            builder.addStatement("values.put($1S, \"\")", name);
                            builder.endControlFlow();
                        }
                        */
                        else if (typeName.contentEquals(Long.class.getName()) || typeName.contentEquals(long.class.getName()))
                        {
                            builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                            builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || 0!=Long.valueOf(cursor.getLong(cursorColIndex)).compareTo(Long.valueOf($1L)))", name);
                            builder.addStatement("values.put($1S, (Long) $1L) ", name);
                            builder.endControlFlow();
                        }
                        else if (typeName.contentEquals(Double.class.getName()) || typeName.contentEquals(double.class.getName()))
                        {
                            builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                            builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || 0!=Double.valueOf(cursor.getDouble(cursorColIndex)).compareTo(Double.valueOf($1L)))", name);
                            builder.addStatement("values.put($1S, (Double) $1L) ", name);
                            builder.endControlFlow();
                        }
                        else if (typeName.contentEquals(Float.class.getName()) || typeName.contentEquals(float.class.getName()))
                        {
                            builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                            builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || 0!=Float.valueOf(cursor.getFloat(cursorColIndex)).compareTo(Float.valueOf($1L)))", name);
                            builder.addStatement("values.put($1S, (Float) $1L) ", name);
                            builder.endControlFlow();
                        }
                        else if (typeName.contentEquals(Boolean.class.getName()) || typeName.contentEquals(boolean.class.getName()))
                        {
                            builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                            builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || (cursor.getInt(cursorColIndex) == 1? Boolean.TRUE : Boolean.FALSE) != (Boolean) $1L)", name);
                            builder.addStatement("values.put($1S, (Integer) ((Boolean) $1L ? 1 : 0)) ", name);
                            builder.endControlFlow();
                        }
                        else if (typeName.contentEquals(Integer.class.getName()) || typeName.contentEquals(int.class.getName()))
                        {
                            builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                            builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || (cursor.getInt(cursorColIndex) != $1L))", name);
                            builder.addStatement("values.put($1S, $1L) ", name);
                            builder.endControlFlow();
                        }
                        else if (mTypeUtils.isAssignable(type, mModelBasicType.asType()))
                        {
                            builder.beginControlFlow("if (null != $L)", name);
                            builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                            builder.beginControlFlow("if (!bTryUpdate || $1L.isModified(cursor, cursorColIndex))", name);
                            builder.addStatement("$1L.toContentValue(values,$1S)", name);
                            builder.endControlFlow();
                            builder.endControlFlow();
                        }
                        else if (isA(type, ENUM))
                        {
                            builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                            builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || !cursor.getString(cursorColIndex).contentEquals($1L.name()))", name);
                            builder.addStatement("values.put($1S, $1L.name()) ", name);
                            builder.endControlFlow();
                        }
                        else if (typeName.startsWith(HashMap.class.getName()))
                        {
                            builder.beginControlFlow("if (null != $L)", name);
                            builder.addStatement("$1T $2Lgson = new $1T()", mGson, name);
                            builder.addStatement("String $1Lmap = $1Lgson.toJson($1L)", name);
                            builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                            builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || !cursor.getString(cursorColIndex).contentEquals($1Lmap))", name);
                            builder.addStatement("values.put($1S, $1Lmap) ", name);
                            builder.endControlFlow();
                            builder.endControlFlow();
                        }
                        else if (mTypeUtils.isAssignable(type, mModelType.asType()))
                        {
                            builder.beginControlFlow("if (null != $L)", name);
                            builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                            builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || !cursor.getString(cursorColIndex).contentEquals(((Model) $1L).id))", name);
                            builder.addStatement("values.put($1S, ((Model) $1L).getId())", name);
                            builder.endControlFlow();
                            builder.endControlFlow();
                        }
                        else
                        {
                            builder.addStatement("throw new IllegalArgumentException($1S + $2L.toString())", "Type is not supported: ", name);
                            mMessager.printMessage(Diagnostic.Kind.ERROR, "ORMModelProcessor generateGetContentValues [unsupported]:" + type.toString());
                        }
                    }
                    break;

                    case ARRAY:
                    {
                        ArrayType asArrayType = (ArrayType) elem.asType();
                        switch(asArrayType.getComponentType().getKind())
                        {
                            case BYTE:
                                if (DEBUG_generateGetContentValues)
                                    mMessager.printMessage(Diagnostic.Kind.NOTE, "ORMModelProcessor generateGetContentValues [byte array]:" + elem.getSimpleName());
                                builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                                builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || !java.util.Arrays.equals(cursor.getBlob(cursorColIndex),$1L))", name);
                                builder.addStatement("values.put($1S, $1L) ", name);
                                builder.endControlFlow();
                                break;
                        }
                    }
                    break;

                    case BOOLEAN:
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || ((cursor.getInt(cursorColIndex)==1?Boolean.TRUE:Boolean.FALSE) != (Boolean)$1L))", name);
                        builder.addStatement("values.put($1S, (Integer) ((Boolean)$1L ? 1 : 0)) ", name);
                        builder.endControlFlow();
                        break;

                    case LONG:
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || (0 != Long.valueOf(cursor.getLong(cursorColIndex)).compareTo(Long.valueOf((Long)$1L))))", name);
                        builder.addStatement("values.put($1S, (Long) $1L) ", name);
                        builder.endControlFlow();
                        break;

                    case BYTE:
                    case INT:
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || (cursor.getInt(cursorColIndex) != $1L))", name);
                        builder.addStatement("values.put($1S, (Integer) $1L) ", name);
                        builder.endControlFlow();
                        break;

                    case DOUBLE:
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || 0!=Double.valueOf(cursor.getDouble(cursorColIndex)).compareTo(Double.valueOf($1L)))", name);
                        builder.addStatement("values.put($1S, (Double) $1L) ", name);
                        builder.endControlFlow();
                        break;

                    case FLOAT:
                        builder.addStatement("cursorColIndex = cursor.getColumnIndex($S)", name);
                        builder.beginControlFlow("if (!bTryUpdate || cursor.isNull(cursorColIndex) || 0!=Float.valueOf(cursor.getFloat(cursorColIndex)).compareTo(Float.valueOf($1L)))", name);
                        builder.addStatement("values.put($1S, (Float) $1L) ", name);
                        builder.endControlFlow();
                        break;
                }
            }
        }

        //builder.endControlFlow();
        builder.addStatement("return values");

        return builder.build();
    }

    MethodSpec generateCopy(ModelSet set)
    {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("copy")
                .addParameter(ParameterSpec.builder(mModel, "source").build())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC);

        builder.beginControlFlow("if (source instanceof $T)", set.mRoot);
        builder.addStatement("$1T m = ($1T)source", set.mRoot);
        for (Element elem : set.mElements)
        {
            String name = elem.getSimpleName().toString();
            builder.addStatement("$1L = m.$1L", name);
        }
        builder.endControlFlow();

        return builder.build();
    }

    MethodSpec generateSave(ModelSet set)
    {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("save")
                .addParameter(ParameterSpec.builder(mString, "where").build())
                .addParameter(ParameterSpec.builder(boolean.class, "bSaveParentsFirst").build())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC);

        builder.beginControlFlow("if (bSaveParentsFirst)");
        builder.addStatement("saveThis(where)");
        builder.addStatement("saveChildren(where, bSaveParentsFirst)");
        builder.endControlFlow();
        builder.beginControlFlow("else");
        builder.addStatement("saveChildren(where, bSaveParentsFirst)");
        builder.addStatement("saveThis(where)");
        builder.endControlFlow();

        return builder.build();
    }

    MethodSpec generateSaveChildren(ModelSet set)
    {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("saveChildren")
                .addParameter(ParameterSpec.builder(mString, "where").build())
                .addParameter(ParameterSpec.builder(boolean.class, "bSaveParentsFirst").build())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC);

        TypeMirror model = mElementUtils.getTypeElement("com.myfiziq.sdk.db.Model").asType();

        for (Element elem : set.mElements)
        {
            Persistent persistent = elem.getAnnotation(Persistent.class);
            //mMessager.printMessage(Diagnostic.Kind.NOTE, String.format("generateSave (%s) %s",
            //                                                           set.mRoot.getSimpleName().toString(), elem.getSimpleName().toString()));

            if ((null != persistent) && (!persistent.pk()) && (!persistent.fromParent()) && (!persistent.fromParentValue()) && (!persistent.asReference()))
            {
                TypeMirror type = elem.asType();
                String name = elem.getSimpleName().toString();

                switch (type.getKind())
                {
                    case DECLARED:
                    {
                        String typeName = type.toString();

                        if (typeName.startsWith(ArrayList.class.getName()))
                        {
                            type = ((DeclaredType) type).getTypeArguments().get(0);
                            if (mTypeUtils.isAssignable(type, model))
                            {
                                builder.beginControlFlow("for ($T m : $L)", mModel, name);
                                builder.beginControlFlow("if (null != m)");
                                builder.addStatement("m.save(where, bSaveParentsFirst)", mModel);
                                builder.endControlFlow();
                                builder.endControlFlow();
                            }
                        }
                        else if (mTypeUtils.isAssignable(type, model))
                        {
                            builder.beginControlFlow("if (null != $L)", name);
                            builder.addStatement("$L.save(where, bSaveParentsFirst)", name);
                            builder.endControlFlow();
                        }
                    }
                    break;
                }
            }
        }

        //mMessager.printMessage(Diagnostic.Kind.NOTE, String.format("generateSave next"));
        return builder.build();
    }

    MethodSpec generateSaveThis(ModelSet set)
    {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("saveThis")
                .addParameter(ParameterSpec.builder(mString, "where").build())
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC);

        builder.beginControlFlow("try");
        builder.addStatement("$T.dbFromModel(this.getClass()).saveModel(this, where)", mOGNTable);
        builder.addStatement("$1T cached = $2T.getAnno(getClass(), $1T.class)", mCached, mOrm);
        builder.beginControlFlow("if (null != cached && cached.cached())");
        builder.addStatement("$T.getInstance().updateModel(getClass(), this)", mOgnDbCache);
        builder.endControlFlow();
        builder.endControlFlow();
        builder.beginControlFlow("catch (Exception e)");
        builder.endControlFlow();

        return builder.build();
    }
}
