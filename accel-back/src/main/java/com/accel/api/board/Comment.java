package com.accel.api.board;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
	private Integer commentId;
	private String userId;
	private String content;
	private String createdAt;
	private String updatedAt;
	private Integer articleId;
	private Integer likeCount; // 댓글 좋아요 수
	private Boolean liked; // 요청 사용자의 좋아요 여부 (응답 전용; 요청 본문에 없어도 역직렬화되도록 래퍼 타입 사용)
}
