package com.accel.api.user;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.accel.api.board.Board;
import com.accel.api.storage.GcsStorageService;

@Service
public class UserServiceImpl implements UserService {

	private final UserDao userDao;
	private final GcsStorageService gcsStorageService;

	public UserServiceImpl(UserDao userDao, GcsStorageService gcsStorageService) {
		this.userDao = userDao;
		this.gcsStorageService = gcsStorageService;
	}

	@Override
	public List<User> getAllUsers() {
		return userDao.selectAllUsers();
	}

	@Override
	public User getById(String userId) {
		return userDao.selectById(userId);
	}

	@Override
	public User getMyPage(String userId) {
		User user = userDao.selectById(userId);
		if (user == null) {
			return null;
		}
		user.setPassword(null);
		return user;
	}

	@Override
	@Transactional
	public User updateMyPage(String userId, UserUpdateRequest request) {
		try {
			return updateMyPage(userId, request, null);
		} catch (IOException e) {
			throw new IllegalStateException("Profile image update failed.", e);
		}
	}

	@Override
	@Transactional
	public User updateMyPage(String userId, UserUpdateRequest request, MultipartFile profileImage) throws IOException {
		User user = updateUserFields(userId, request);

		if (profileImage != null && !profileImage.isEmpty()) {
			user = replaceProfileImage(user, profileImage);
		}

		user.setPassword(null);
		return user;
	}

	@Override
	@Transactional
	public User updateProfileImage(String userId, MultipartFile profileImage) throws IOException {
		if (profileImage == null || profileImage.isEmpty()) {
			throw new IllegalArgumentException("Profile image is required.");
		}

		User user = userDao.selectById(userId);
		if (user == null) {
			return null;
		}

		user = replaceProfileImage(user, profileImage);
		user.setPassword(null);
		return user;
	}

	private User updateUserFields(String userId, UserUpdateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Update request is required.");
		}

		User user = userDao.selectById(userId);
		if (user == null) {
			return null;
		}

		String newName = requireNonBlankIfPresent(request.getName(), "name");
		String newNickname = requireNonBlankIfPresent(request.getNickname(), "nickname");
		Integer newAge = request.getAge();
		String newEmail = trimNullable(request.getEmail());

		if (newAge != null && (newAge < 0 || newAge > 150)) {
			throw new IllegalArgumentException("Age must be between 0 and 150.");
		}

		if (newNickname != null && !newNickname.equals(user.getNickname())
				&& userDao.countByNickname(newNickname) > 0) {
			throw new DuplicateNicknameException(newNickname);
		}

		if (newName != null) {
			user.setName(newName);
		}
		if (newNickname != null) {
			user.setNickname(newNickname);
		}
		if (newAge != null) {
			user.setAge(newAge);
		}
		if (request.getEmail() != null) {
			user.setEmail(newEmail);
		}

		userDao.updateUser(user);
		return user;
	}

	private User replaceProfileImage(User user, MultipartFile profileImage) throws IOException {
		String previousProfileImg = user.getProfileImg();
		String nextProfileImg = gcsStorageService.uploadProfileImage(profileImage);

		if (!StringUtils.hasText(nextProfileImg)) {
			return user;
		}

		user.setProfileImg(nextProfileImg);

		try {
			userDao.updateUser(user);
		} catch (RuntimeException e) {
			gcsStorageService.deleteProfileImage(nextProfileImg);
			throw e;
		}

		gcsStorageService.deleteProfileImage(previousProfileImg);
		return user;
	}

	@Override
	@Transactional
	public void deleteMyPage(String userId) {
		userDao.deleteCommentLikesOfUser(userId);
		userDao.deleteArticleLikesOfUser(userId);
		userDao.deleteCommentsOfUser(userId);
		userDao.deleteArticlesOfUser(userId);
		userDao.deleteUser(userId);
	}

	@Override
	public List<Board> getMyBoards(String userId) {
		return userDao.selectMyBoards(userId);
	}

	@Override
	public List<Board> getMyCommentedBoards(String userId) {
		return userDao.selectMyCommentedBoards(userId);
	}

	@Override
	public List<Board> getMyLikedBoards(String userId) {
		return userDao.selectMyLikedBoards(userId);
	}

	@Override
	public UserProfileResponse getUserProfile(String userId) {
		User user = userDao.selectById(userId);
		if (user == null) {
			return null;
		}
		List<Board> posts = userDao.selectMyBoards(userId);
		return new UserProfileResponse(user.getNickname(), user.getProfileImg(), posts);
	}

	private String requireNonBlankIfPresent(String value, String fieldName) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " cannot be blank.");
		}
		return trimmed;
	}

	private String trimNullable(String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
