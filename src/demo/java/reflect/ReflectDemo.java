package demo.java.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ReflectDemo {
    static Class<?> getRawType(Type type) {
        Objects.requireNonNull(type, "type == null");

        if (type instanceof Class<?>) {
            System.out.println("is Class");
            // Type is a normal class.
            return (Class<?>) type;
        }
        // 参数类型
        if (type instanceof ParameterizedType) {
            System.out.println("is ParameterizedType");
            ParameterizedType parameterizedType = (ParameterizedType) type;

            // I'm not exactly sure why getRawType() returns Type instead of Class. Neal isn't either but
            // suspects some pathological case related to nested classes exists.
            Type rawType = parameterizedType.getRawType();
            if (!(rawType instanceof Class)) throw new IllegalArgumentException();
            return (Class<?>) rawType;
        }
        // 泛型数组
        if (type instanceof GenericArrayType) {
            System.out.println("is GenericArrayType");
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            return Array.newInstance(getRawType(componentType), 0).getClass();
        }
        if (type instanceof TypeVariable) {
            System.out.println("is TypeVariable");
            // We could use the variable's bounds, but that won't work if there are multiple. Having a raw
            // type that's more general than necessary is okay.
            return Object.class;
        }
        if (type instanceof WildcardType) {
            System.out.println("is WildcardType");
            return getRawType(((WildcardType) type).getUpperBounds()[0]);
        }

        throw new IllegalArgumentException(
                "Expected a Class, ParameterizedType, or "
                        + "GenericArrayType, but <"
                        + type
                        + "> is of type "
                        + type.getClass().getName());
    }

    interface ReflectTest<T, U extends Object> {
        // type instanceof ParameterizedType
        List<String> getUserIds();

        // type instanceof GenericArrayType
        List<String>[] getUserNames();

        // type instanceof TypeVariable
        T getUserType();

        List<?> getParameters();
    }

    public static void main(String[] args) throws NoSuchFieldException {
        System.out.println(getRawType(ReflectDemo.class));

        ReflectTest reflectTest = (ReflectTest) Proxy.newProxyInstance(ReflectDemo.class.getClassLoader(), new Class[]{ReflectTest.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                System.out.println("________begin_________");
                System.out.println("RetrunType.typeName:" + method.getGenericReturnType().getTypeName());
                System.out.println(getRawType(method.getGenericReturnType()));
                //System.out.println(method.getGenericParameterTypes());
                System.out.println("________end_________");
                return null;
            }
        });
        reflectTest.getUserIds();
        reflectTest.getUserNames();
        reflectTest.getUserType();
        reflectTest.getParameters();

        /*printFieldType(ArrayBean.class, "arrayType");
        System.out.println("\n");
        printFieldType(ArrayBean.class, "genericArrayType");
        System.out.println("\n");
        printFieldType(ArrayBean.class, "genericMultiArrayType");
        System.out.println("\n");
        printFieldType(ArrayBean.class, "specialMultiArrayType");*/
    }

    private static void printFieldType(Class<?> clazz, String fieldName)
            throws NoSuchFieldException, SecurityException {
        System.out.println("________beginField:" + fieldName + "________");
        Field field = clazz.getField(fieldName);
        System.out.println("Declared class: " + field.getDeclaringClass());
        // 带具体泛型参数类型
        Type genericType = field.getGenericType();
        System.out.println("Generic type: " + genericType.getTypeName());
        // 不带具体的泛型参数类型
        Type type = field.getType();
        System.out.println("Type: " + type.getTypeName());
        if (isGenericArrayType(genericType)) {
            printGenericFieldType((GenericArrayType) genericType);
        }
        if (isParameterizedType(genericType)) {
            printParameterizedType((ParameterizedType) genericType);
        }
        System.out.println("________endField:" + fieldName + "________");
    }

    private static void printGenericFieldType(GenericArrayType genericArrayType) {
        Type componentType = genericArrayType.getGenericComponentType();
        System.out.println("Component type of : "
                + genericArrayType.getTypeName() + " is "
                + componentType.getTypeName());
        if (isGenericArrayType(componentType)) {
            printGenericFieldType((GenericArrayType) componentType);
        }
        if (isParameterizedType(componentType)) {
            printParameterizedType((ParameterizedType) componentType);
        }
    }

    private static boolean isGenericArrayType(Type type) {
        if (GenericArrayType.class.isAssignableFrom(type.getClass())) {
            System.out.println("Is GenericArrayType ? true");
            return true;
        }
        return false;
    }

    private static boolean isParameterizedType(Type type) {
        if (ParameterizedType.class.isAssignableFrom(type.getClass())) {
            System.out.println("Is ParameterizedType ? true");
            return true;
        }

        return false;
    }

    private static void printParameterizedType(ParameterizedType paramType) {
        System.out.println("Parameterized type details of " + paramType);
        System.out.println("Type name: " + paramType.getTypeName());
        System.out.println("Raw type: " + paramType.getRawType());
        System.out.println("Actual type arguments: "
                + Arrays.asList(paramType.getActualTypeArguments()));
        for (Type type : paramType.getActualTypeArguments()) {
            if (isParameterizedType(type)) {
                printParameterizedType((ParameterizedType) type);
            }
        }
    }
}

class SomeBean<P, Q> {
}

class ArrayBean {
    public List[] arrayType;
    public List<String>[] genericArrayType;
    public List<String>[][] genericMultiArrayType;
    public List<SomeBean<String, Integer>>[][] specialMultiArrayType;
}
