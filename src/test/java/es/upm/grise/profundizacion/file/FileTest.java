package es.upm.grise.profundizacion.file;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;

import es.upm.grise.profundizacion.exceptions.EmptyBytesArrayException;
import es.upm.grise.profundizacion.exceptions.InvalidContentException;
import es.upm.grise.profundizacion.exceptions.WrongFileTypeException;

public class FileTest {

    // -------------------------------
    // Tests para addProperty
    // -------------------------------

    @Test
    void testAddPropertyThrowsInvalidContentExceptionWhenNull() {
        File file = new File();
        file.setType(FileType.PROPERTY);

        assertThrows(InvalidContentException.class, () -> {
            file.addProperty(null);
        });
    }

    @Test
    void testAddPropertyThrowsWrongFileTypeExceptionWhenImage() {
        File file = new File();
        file.setType(FileType.IMAGE);

        char[] content = {'K', '=', 'V'};
        assertThrows(WrongFileTypeException.class, () -> {
            file.addProperty(content);
        });
    }

    @Test
    void testAddPropertyAddsContentCorrectlyWhenProperty() throws Exception {
        File file = new File();
        file.setType(FileType.PROPERTY);

        char[] content = {'D', 'A', 'T', 'E', '=', '2', '0', '2', '5'};
        file.addProperty(content);

        List<Character> stored = file.getContent();
        assertEquals(content.length, stored.size());
        for (int i = 0; i < content.length; i++) {
            assertEquals(content[i], stored.get(i));
        }
    }

    // -------------------------------
    // Tests para getCRC32
    // -------------------------------

    @Test
    void testGetCRC32ReturnsZeroWhenEmpty() throws Exception {
        File file = new File();
        file.setType(FileType.PROPERTY);

        long crc = file.getCRC32();
        assertEquals(0L, crc);
    }

    @Test
    void testGetCRC32DelegatesToFileUtilsAndTransformsChars() throws Exception {
        File file = new File();
        file.setType(FileType.PROPERTY);

        // Añadimos contenido
        char[] content = {'A', 'B'};
        file.addProperty(content);

        try (MockedConstruction<FileUtils> mocked = mockConstruction(FileUtils.class,
                (mock, context) -> {
                    // Simulamos que FileUtils devuelve un valor fijo
                    when(mock.calculateCRC32(any(byte[].class))).thenReturn(12345L);
                })) {

            long crc = file.getCRC32();

            // Verificamos que el valor devuelto es el del mock
            assertEquals(12345L, crc);

            // Verificamos que FileUtils fue construido exactamente una vez
            assertEquals(1, mocked.constructed().size());

            // Capturamos el argumento pasado a calculateCRC32
            FileUtils constructed = mocked.constructed().get(0);
            verify(constructed).calculateCRC32(any(byte[].class));
        }
    }

    @Test
    void testGetCRC32TransformsCharToTwoBytesCorrectly() throws Exception {
        File file = new File();
        file.setType(FileType.PROPERTY);

        // Char con valores conocidos
        char[] content = {0x1234}; // MSB=0x12, LSB=0x34
        file.addProperty(content);

        try (MockedConstruction<FileUtils> mocked = mockConstruction(FileUtils.class,
                (mock, context) -> {
                    when(mock.calculateCRC32(any(byte[].class))).thenReturn(999L);
                })) {

            long crc = file.getCRC32();
            assertEquals(999L, crc);

            FileUtils constructed = mocked.constructed().get(0);

            // Verificamos que el array de bytes contiene los dos bytes correctos
            verify(constructed).calculateCRC32(argThat(bytes -> {
                return bytes.length == 2 &&
                       bytes[0] == 0x12 &&
                       bytes[1] == 0x34;
            }));
        }
    }
}

