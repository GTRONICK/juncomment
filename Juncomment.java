/*JUNCOMMENT 1.1
CREATED:   07/02/2018
LAST EDIT: 20/03/2018
Jaime Quiroga		[gtronick@gmail.com]
Alexander Carvajal 	[alexander.carvajal.lopez@gmail.com]

*/
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase para eliminacion de comentarios en archivos de texto. Soporta XHTML, 
 * JAVA, C, HTML, JS, PHP, entre otros. Ajusta de manera automatica los comentarios
 * obligatorios para manejo de Javascript dentro de los XHTML.
 */
public class Juncomment {

    private static final String gsRegex = "\\G([^\"'/\\\\]*(?:(?:\\\\.|/(?![*/])|\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*')[^\"'/\\\\]*)*+)(?://.*|/\\*[^*]*(?:\\*(?!/)[^*]*)*\\*/)";        
    private static final String gsOpenCdataRegex = "(\\/\\/[ ]*<!\\[CDATA\\[.)|(\\/\\*.*(<!\\[CDATA\\[).*?(\\*\\/))";
    private static final String gsCloseCdataRegex = "(\\/\\/[ ]*\\]\\]>)|(\\/\\*.*(\\]\\]>).*?(\\*\\/))";    
    private static final ArrayList<String> gobFileList = new ArrayList<>();
    
    /**
     * Metodo principal. 
     * @param args Argumentos para la aplicacion.
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws java.lang.Exception {
        
        Juncomment gobJUncomment = new Juncomment();
        ArrayList<String> lobArrayList;
        
        if(args.length == 3 && args[0].equals("-d")){
            System.out.println("Processing input file, and generating output file...");
            gobJUncomment.processFile(args[1], args[2]);
            
        } else if(args.length == 3 && args[0].equals("-f")) {   
            System.out.println("Processing files in folder...");
            lobArrayList = gobJUncomment.listFiles(args[1], args[2]);
            for(String lsListElement:lobArrayList){
                gobJUncomment.processFile(lsListElement, lsListElement);
            }
            
        } else if(args.length == 3 && args[0].equals("-r")) {
            System.out.println("Processing files recursively...");
            gobJUncomment.recursiveListDirectory(args[1], args[2]);
            int liSize = gobFileList.size();
            String lsItem;
            for(int i = 0; i < liSize; i++){
                lsItem = gobFileList.get(i);
                gobJUncomment.processFile(lsItem, lsItem);               
            }
            System.out.println("Done");
            
        } else if(args.length == 2 && args[0].equals("-s")) {
            System.out.println("Processing file ...");
            gobJUncomment.processFile(args[1], args[1]);
            
        }else if((args.length == 1 && args[0].equals("-h"))){
            gobJUncomment.showHelp();
            
        }else{
            System.out.println("\nWrong parameters, use java -jar juncomment.jar -h to show the help.");
        }
    }

    
    /**
     * Lee un archivo y retorna su contenido en un String.
     * @param asFileName Ruta completa del archivo.
     * @return Contenido del archivo.
     */
    public String readFromFile(String asFileName) {
        
        String lsText = "";
        File lobFile;
        FileReader lobReader = null;
        String lsReadLine;

        try {
            lobFile = new File(asFileName);
            lobReader = new FileReader(lobFile);
            
            try (BufferedReader lobBufferedReader = new BufferedReader(lobReader)) {
                
                while ((lsReadLine = lobBufferedReader.readLine()) != null) {
                    
                    lsText = lsText.concat(lsReadLine.concat("\r\n"));
                }
                
                lobReader.close();
            }

        } catch (FileNotFoundException ex) {
            System.out.println("\nFile Not Found!, check the path: \n" + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("\nCan't read file. Check file permissions: \n" + ex.getMessage());
        } finally {
            try {
                if(lobReader != null) lobReader.close();
            } catch (IOException | NullPointerException ex) {
                System.out.println("\n\nFile not found, or access denied.");
            }
        }
       
        return lsText;
    }
    
    /**
     * Convierte una cadena de texto, a un arreglo donde cada elemento,
     * corresponde a una linea del texto.
     * @param asString Texto a procesar.
     * @return Arreglo con las lineas del texto.
     */
    public ArrayList<String> stringToArray(String asString){
                
        String [] lobString = asString.split("\r\n");
        ArrayList<String> lobStringList = new ArrayList<>(Arrays.asList(lobString));
        return lobStringList;
    }
    
    /**
     * Crea un archivo en la ruta especificada, con el texto contenido en 
     * los elementos del arreglo.
     * @param asFilePath Ruta donde guardar el archivo.
     * @param aobArray Texto del archivo separado en lineas.
     * @return true si el guardado fue exitoso, false de lo contrario.
     */
    public boolean saveToFile(String asFilePath, ArrayList<String> aobArray){
        BufferedWriter lobBufferedWriter = null;
        FileWriter lobFileWriter = null;
        
        try {
            
            lobFileWriter = new FileWriter(asFilePath);
            lobBufferedWriter = new BufferedWriter(lobFileWriter);
            
            for (String lsArrayElement : aobArray) {
                lobBufferedWriter.write(lsArrayElement);
                lobBufferedWriter.append("\r\n");
            }

        } catch (IOException e) {
            
            System.out.println("ERROR: " + e.getLocalizedMessage());
            
        } finally {

            try {

                if (lobBufferedWriter != null) lobBufferedWriter.close();

                if (lobFileWriter != null) lobFileWriter.close();

            } catch (IOException ex) {
                
                System.out.println("ERROR: " + ex.getLocalizedMessage());
                return false;
            }
        }
        
        return true;
    }    
    
    /**
     * Recupera los nombres de los archivos que pertenezcan a la extension
     * especificada, y esten dentro de la carpeta especificada.
     * @param asFolder Carpeta a procesar.
     * @param asExtension Extension de archivo a buscar.
     * @return Lista de nombres de archivo que cumplen con la condicion.
     */
    public ArrayList<String> listFiles(String asFolder, String asExtension) {

        GenericExtFilter lobGenericFilter = new GenericExtFilter(asExtension);

        File lobDir = new File(asFolder);

        if(lobDir.isDirectory() == false){
            System.out.println("Directory does not exists : " + asFolder);
            return null;
        }
        
        String[] lobList = lobDir.list(lobGenericFilter);
        
        ArrayList<String> lobStringList = new ArrayList<>();

        for (String lsFile : lobList) {
            String lsTemporal = new StringBuffer(asFolder).append(File.separator).append(lsFile).toString();
            lobStringList.add(lsTemporal);
        }
        
        return lobStringList;
    }

    /**
     * Clase para manejo del filtro para la extensión del archivo.
     */
    public static class GenericExtFilter implements FilenameFilter {

        private final String gsExtension;

        /**
         * Constructor de la clase
         * @param asExtension Extension de archivo a procesar.
         */
        public GenericExtFilter(String asExtension) {
                this.gsExtension = asExtension;
        }

        /**
         * Filtro que valida el nombre de un archivo
         * @param dir Directorio a escanear.
         * @param name Nombre del archivo a procesar.
         */
        @Override
        public boolean accept(File dir, String name) {
                return (name.endsWith(gsExtension));
        }
    }
    
    /**
     * Procesa un archivo, eliminando los comentarios encontrados y generando un
     * archivo de salida cuando se usa el paramentro -d. De lo contrario, se
     * sobreescribe el archivo de entrada.
     * @param asInputFile Archivo de entrada.
     * @param asOutputFile Archivo de salida.
     */
    public void processFile(String asInputFile, String asOutputFile){
        
        String lsFileText = readFromFile(asInputFile);

        Pattern lobPattern = Pattern.compile(gsOpenCdataRegex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher lobMatcher = lobPattern.matcher(lsFileText);
        String lsResult = lobMatcher.replaceAll("JUC_NM_0");
        
        lobPattern = Pattern.compile(gsCloseCdataRegex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        lobMatcher = lobPattern.matcher(lsResult);
        lsResult = lobMatcher.replaceAll("JUC_NM_1");
        
        lobPattern = Pattern.compile(gsRegex, Pattern.COMMENTS);
        lobMatcher = lobPattern.matcher(lsResult);
        lsResult = lobMatcher.replaceAll("$1");
        
        lsResult = lsResult.replace("JUC_NM_0","//<![CDATA[");
        lsResult = lsResult.replace("JUC_NM_1","//]]>");

        lobPattern = Pattern.compile("(?s)<!--(.*?)-->", Pattern.COMMENTS);
        lobMatcher = lobPattern.matcher(lsResult);
        lsResult = lobMatcher.replaceAll("");

        saveToFile(asOutputFile, stringToArray(lsResult));
    }
    
    /**
     * Muestra la ayuda de la aplicacion, cuando se usa el parametro -h, o cuando
     * se introducen parametros incorrectos.
     */
    private void showHelp(){
        
        System.out.println("");
        System.out.println("");
        System.out.println("------------------------------------------------------------------------------------------------");
        System.out.println("                                           JUNCOMMENT 1.1                                       ");
        System.out.println("------------------------------------------------------------------------------------------------");
        System.out.println("                                                                                                ");
        System.out.println("  This application deletes C comments like // and /**/, and xml comments like <!--  --> in the  ");
        System.out.println("  file, or  files inside the specified folder.                                                  ");
        System.out.println("                                                                                                ");
        System.out.println("------------------------------------------------------------------------------------------------");
        System.out.println("                                                                                                ");
        System.out.println("  USAGE: java -jar juncomment.jar [OPTION] INPUT_FILE_OR_FOLDER [OUTPUT_FILE_OR_EXTENSION]      ");
        System.out.println("                                                                                                ");
        System.out.println("  OPTIONS:                                                                                      ");
        System.out.println("                                                                                                ");
        System.out.println("    -d  (duplicate) Deletes the comments in the input file and generates an output file         ");
        System.out.println("                                                                                                ");
        System.out.println("    -s  (self) Deletes the comments in the input file and overwrites it.                        ");
        System.out.println("                                                                                                ");
        System.out.println("    -f  (folder) Like -s, but for all the files inside the specified folder that match          ");
        System.out.println("        the especified extension.                                                               ");
        System.out.println("                                                                                                ");
        System.out.println("    -r  (recursive) Like -f, but also for subfolders in a recursive fashion.                    ");
        System.out.println("                                                                                                ");
        System.out.println("  Examples:                                                                                     ");
        System.out.println("                                                                                                ");
        System.out.println("  java -jar juncomment.jar -d \"C:\\Documents\\file_in.xhtml\" \"C:\\Documents\\file_out.xhtml\"");
        System.out.println("                                                                                                ");
        System.out.println("  java -jar juncomment.jar -s \"C:\\Documents\\file_in.xhtml\"                                  ");
        System.out.println("                                                                                                ");
        System.out.println("  java -jar juncomment.jar -f \"C:\\Folder\" \"java\"                                           ");
        System.out.println("                                                                                                ");
        System.out.println("  java -jar juncomment.jar -r \"C:\\Folder\" \"xhtml\"                                          ");
        System.out.println("                                                                                                ");
    }
    
    /**
     * Lista el contenido de un directorio recursivamente, y almacena en un
     * arreglo global únicamente los archivos encontrados que pertenezcan a la
     * extensión de archivo especificada.
     * @param asPath Ruta de la carpeta a examinar.
     * @param asExtension Extension de archivo a buscar.
     */
    public void recursiveListDirectory(String asPath, String asExtension) {
        
        File lobPath = new File(asPath);
        ArrayList<String> lobTempList;
        
        if( lobPath.exists() ) {
            File[] files = lobPath.listFiles();
            for (File file : files) {
                 
                if (file.isDirectory()) {
                    lobTempList = listFiles(file.getAbsolutePath(), asExtension);
                    for(String lsListItem:lobTempList){
                        gobFileList.add(lsListItem);
                    }
                    recursiveListDirectory(file.getAbsolutePath(),asExtension);
                } else {
                    gobFileList.add(file.getAbsolutePath());
                }
            }
        }
    }
}
