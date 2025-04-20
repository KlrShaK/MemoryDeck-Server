package ch.uzh.ifi.hase.soprafs24.service;

import com.google.cloud.storage.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class GoogleCloudStorageServiceTest {

    @Mock
    private Storage storage;

    @InjectMocks
    private GoogleCloudStorageService gcsService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetFileFromBucket_returnsBlob() {
        // Arrange
        String filename = "test-file.jpg";
        BlobId blobId = BlobId.of("memory-deck-storage", filename);
        Blob expectedBlob = mock(Blob.class);

        when(storage.get(blobId)).thenReturn(expectedBlob);

        // Act
        Blob actualBlob = gcsService.getFileFromBucket(filename);

        // Assert
        assertEquals(expectedBlob, actualBlob);
        verify(storage, times(1)).get(blobId);
    }

    @Test
    void testUploadFile_returnsUrl() {
        // Arrange
        String fileName = "upload-test.jpg";
        byte[] fileBytes = "hello world".getBytes();
        BlobId blobId = BlobId.of("memory-deck-storage", fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        when(storage.create(blobInfo, fileBytes)).thenReturn(mock(Blob.class));

        // Act
        String resultUrl = gcsService.uploadFile(fileBytes, fileName);

        // Assert
        String expectedUrl = "https://storage.googleapis.com/memory-deck-storage/upload-test.jpg";
        assertEquals(expectedUrl, resultUrl);
        verify(storage, times(1)).create(any(BlobInfo.class), eq(fileBytes));
    }

    // @Test
    // void testDeleteFile_existingBlob_returnsTrue() {
    //     // Arrange
    //     String fileName = "delete-me.jpg";
    //     String imageUrl = "https://storage.googleapis.com/memory-deck-storage/" + fileName;
    
    //     Blob mockBlob = mock(Blob.class);
    //     when(storage.get("memory-deck-storage", fileName)).thenReturn(mockBlob);
    //     when(mockBlob.delete()).thenReturn(true);
    
    //     // Act
    //     boolean deleted = gcsService.deleteFile(imageUrl);
    
    //     // Assert
    //     assertTrue(deleted);
    //     verify(mockBlob).delete();
    // }
    

    @Test
    void testDeleteFile_fileNotFound_returnsFalse() {
        // Arrange
        String fileName = "not-found.jpg";
        String imageUrl = "https://storage.googleapis.com/memory-deck-storage/" + fileName;

        // Act
        boolean deleted = gcsService.deleteFile(imageUrl);

        // Assert
        assertFalse(deleted);
    }
}
