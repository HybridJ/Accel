package com.accel.api.board;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Board {
	private Integer articleId; // 게시글 고유번호
	private String userId; // 게시글 작성자 id
	private String title; // 제목
	private String content; // 내용
	private String imageUrl; // 게시글 이미지 URL
	private List<String> imageUrls;
	private String createdAt; // 게시일
	private String updatedAt; // 수정일
	private Boolean testDriveAvailable; // 시승 가능 여부 (요청 본문에 없어도 역직렬화되도록 래퍼 타입; insert 전 null→false 정규화)
	private Integer viewCnt; // 조회수
	private Integer likeCount;
	private Integer brandId; // 브랜드 고유번호
	private String boardName; // 게시판명
	private String boardNameKo; // 게시판명 (한글)
	private String brandSlug; // 브랜드 슬러그 (URL용)
	private Boolean isDomestic;
}

