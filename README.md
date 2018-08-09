# SunTinker
   仿Tinker实现核心原理

#### 核心原理
        android程序运行时通过PathClassLoader加载需要的class文件，而class文件分别被
    保存dex中，PathClassLoader继承自BaseDexClassLoader,在BaseDexClassLoader
    中有个private的DexPathList，DexPathList中的dexElements里面保存了该apk下所有的dex文件。
        当PathClassLoader加载目标class时，就去这些个dex中查找，查找到了就保存起来，
    下次直接使用。

        所以要在程序一开始的时候就将修复的dex文件插入到elements的最前面，这样的话
    当PathClassLoader工作时在前面找到了已经修改的class，并拿取使用，就不会再找后面
    有问题的class了。

#### 缺点
   1. 由于所有的代码都是在java层，所以都是通过反射去查找和替换elements的。
   2. 由于pathClassLoader一旦从dex中找到后下次就不会再去dex中找了，所以要把修复
   工作放在程序的最开始执行，可能导致启动是变慢。

#### 和Tinker差别
   1. Tinker是通过俩个apk文件生成一个差分dex，在修复的时候是将这个查分dex和原dex
   进行合并操作生成一个全新的dex。而我的是直接将dex插入到element数组中。
   2. Tinker的查分方式可以用于修补java文件，资源文件，甚至so文件。
   3. 我只是仿照Tinker实现了一些核心的功能，如果要使用的话，请还是使用Tinker，毕竟
   强大的微信都在用这个，至少适配好....


#### 代码

     /**
         * 修复方法，
         * @param application application
         * @param dexPath     存放补丁dex的路径
         * @return 是否修复成功
         */
        public boolean fix(Context application, String dexPath){
            try {
                //application中的getClassLoader是pathClassLoader
                //而 pathClassLoader 继承 BaseDexClassLoader
                ClassLoader classLoader = application.getClassLoader();
                // 获取  BaseDexClassLoader 的 private final DexPathList pathList;
                Field pathListField = ReflexUtils.findField(classLoader, "pathList");
                Object pathList = pathListField.get(classLoader);
    //            获取 DexPathList中 private Element[] dexElements;
                Field dexElementsField = ReflexUtils.findField(pathList, "dexElements");
                Object[] dexElements = (Object[]) dexElementsField.get(pathList);

                //将dexPath中的dex文件加载成element
                Object[] addElements = dexFile2Elements(dexPath, pathList);

                if (addElements == null){
                    return false;
                }
                //创建新的 Element[]
                Object[] newElements = (Object[]) Array.newInstance(dexElements.getClass().getComponentType(),
                        dexElements.length + addElements.length);
               System.arraycopy(addElements, 0, newElements, 0, addElements.length);
               System.arraycopy(dexElements, 0, newElements, addElements.length, dexElements.length);

                //替换DexPathList中的dexElements
                dexElementsField.set(pathList, newElements);

                return true;
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return false;
        }

        /**
         * dexPath转element数组
         * @param dexPath
         * @param pathList
         */
        private Object[] dexFile2Elements(String dexPath,  Object pathList) throws NoSuchMethodException,
                InvocationTargetException, IllegalAccessException {
            File dexFile = new File(dexPath);
            File[] listFiles = dexFile.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".dex");
                }
            });
            if (listFiles == null || listFiles.length == 0){
                return null;
            }
            List<File> fileList = Arrays.asList(listFiles);

            //获取makeDexElement方法
            Method makeDexElement;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                //5.0 - 6.0
                makeDexElement = ReflexUtils.findMethod(pathList, "makeDexElements",
                        ArrayList.class, File.class, ArrayList.class);
            }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                //大于 6.0
                makeDexElement = ReflexUtils.findMethod(pathList, "makePathElements",
                        List.class, File.class, List.class);
            }else{
                return null;
            }
            ArrayList<IOException> suppressedExceptions = new ArrayList<>();
            return (Object[]) makeDexElement
                    .invoke(pathList, fileList, null, suppressedExceptions);
        }

其中ReflexUtils

    /**
     * -- 反射工具类
     * <p>
     * Created by sunxy on 2018/8/7 0007.
     */
    public class ReflexUtils {

    /**
     * 反射获得 指定对像中的成员
     * 找不到的话就去他的 父类 中找
     */
    public static Field findField(Object instance, String name) throws NoSuchFieldException{
        Class<?> clazz = instance.getClass();
        while (clazz != null){
            try {
                Field field = clazz.getDeclaredField(name);
                if (!field.isAccessible()){
                    field.setAccessible(true);
                }
                return field;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + name + " not found in " + instance.getClass());
    }

    /**
     * 反射获取对象中的指定函数
     *
     */
    public static Method findMethod(Object instance, String name, Class... parameterTypes)
    throws NoSuchMethodException{
        Class<?> clazz = instance.getClass();
        while (clazz != null){
            try {
                Method method = clazz.getDeclaredMethod(name, parameterTypes);
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                return method;
            } catch (NoSuchMethodException e) {
                //如果找不到往父类找
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchMethodException("Method " + name + " with parameters " + Arrays.asList
                (parameterTypes) + " not found in " + instance.getClass());
    }

