package com.accel.api.board;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentLikeStatus {
	private int likeCount; // 댓글 좋아요 수
	private boolean liked; // 요청 사용자의 좋아요 여부
}
