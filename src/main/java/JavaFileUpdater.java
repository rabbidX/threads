import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;

public class JavaFileUpdater {

    public static void main(String[] args) {
        String directoryPath = "C:\\Users\\Rabbid\\projects\\threads\\src";
        String oldString = "me.garyanov";
        String newString = "me.garyanov";

        try {
            updateJavaFilesWithStream(directoryPath, oldString, newString);
            System.out.println("Обновление завершено успешно!");
        } catch (IOException e) {
            System.err.println("Ошибка при обработке файлов: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void updateJavaFilesWithStream(String directoryPath, String oldString, String newString) throws IOException {
        Path startDir = Paths.get(directoryPath);

        try (Stream<Path> paths = Files.walk(startDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(filePath -> {
                        try {
                            processFile(filePath, oldString, newString);
                        } catch (IOException e) {
                            System.err.println("Ошибка при обработке файла " + filePath + ": " + e.getMessage());
                        }
                    });
        }
    }

    private static void processFile(Path filePath, String oldString, String newString) throws IOException {
        String content = Files.readString(filePath);

        if (content.contains(oldString)) {
            System.out.println("Обрабатываю файл: " + filePath);

            String updatedContent = content.replace(oldString, newString);
            Files.writeString(filePath, updatedContent);

            long count = content.chars()
                    .mapToObj(c -> (char) c)
                    .filter(c -> content.contains(oldString))
                    .count();
            System.out.println("  Заменено строк в файле: " + count);
        }
    }
}