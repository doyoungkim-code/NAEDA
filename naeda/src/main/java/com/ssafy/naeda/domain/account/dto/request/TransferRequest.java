package com.ssafy.naeda.domain.account.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransferRequest {

    @NotNull(message = "출금 계좌번호는 필수입니다.")
    @NotBlank(message = "출금 계좌번호는 필수입니다.")
    private String withdrawalAccountNo;

    @NotNull(message = "입금 계좌번호는 필수입니다.")
    @NotBlank(message = "입금 계좌번호는 필수입니다.")
    private String depositAccountNo;

    @NotNull(message = "이체 금액은 필수입니다.")
    @Min(value = 1, message = "이체 금액은 1원 이상이어야 합니다.")
    @Max(value = 100_000_000, message = "이체 금액은 1억원 이하이어야 합니다.")
    private Long amount;

    @Size(max = 255, message = "메모는 255자 이하이어야 합니다.")
    private String memo;
}
