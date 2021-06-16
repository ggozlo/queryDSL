package study.queryDSL.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data

@NoArgsConstructor
public class MemberDto {
    private String username;
    private int age;

    @QueryProjection // dto도 Q파일로 생성
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
