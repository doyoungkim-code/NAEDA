package com.ssafy.naeda.domain.face.controller;

import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
import com.ssafy.naeda.domain.face.service.FaceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class FaceControllerTest {

    @InjectMocks
    private FaceController faceController;

    @Mock
    private FaceService faceService;

    @Test
    @DisplayName("search: topK/amount 미입력 시 기본값(3, 0)으로 FaceService를 호출한다")
    void search_withDefaultValues() {
        MockMultipartFile image = new MockMultipartFile("image", "face.jpg", "image/jpeg", new byte[]{1, 2, 3});
        given(faceService.search(any(), anyInt(), anyLong())).willReturn(SearchResponse.builder().build());

        ResponseEntity<SearchResponse> response = faceController.search(image, null, null);

        ArgumentCaptor<Integer> topKCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        then(faceService).should().search(any(), topKCaptor.capture(), amountCaptor.capture());
        assertThat(topKCaptor.getValue()).isEqualTo(3);
        assertThat(amountCaptor.getValue()).isEqualTo(0L);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("search: topK/amount 입력값을 숫자로 파싱해 FaceService를 호출한다")
    void search_withProvidedValues() {
        MockMultipartFile image = new MockMultipartFile("image", "face.jpg", "image/jpeg", new byte[]{1, 2, 3});
        given(faceService.search(any(), anyInt(), anyLong())).willReturn(SearchResponse.builder().build());

        faceController.search(image, "5", "70000");

        ArgumentCaptor<Integer> topKCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Long> amountCaptor = ArgumentCaptor.forClass(Long.class);
        then(faceService).should().search(any(), topKCaptor.capture(), amountCaptor.capture());
        assertThat(topKCaptor.getValue()).isEqualTo(5);
        assertThat(amountCaptor.getValue()).isEqualTo(70_000L);
    }
}
