package com.netflix.clone.serviceImpl;

import com.netflix.clone.dao.UserRepository;
import com.netflix.clone.dao.VideoRepository;
import com.netflix.clone.dto.response.MessageResponse;
import com.netflix.clone.dto.response.PageResponse;
import com.netflix.clone.dto.response.VideoResponse;
import com.netflix.clone.entity.User;
import com.netflix.clone.entity.Video;
import com.netflix.clone.service.WatchlistService;
import com.netflix.clone.util.PaginationUtils;
import com.netflix.clone.util.ServiceUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class WatchlistServiceImpl implements WatchlistService {

    private UserRepository userRepository;

    private VideoRepository videoRepository;

    private ServiceUtils serviceUtils;

    public WatchlistServiceImpl(UserRepository userRepository, VideoRepository videoRepository,  ServiceUtils serviceUtils) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.serviceUtils = serviceUtils;
    }

    @Override
    public MessageResponse addToWatchlist(String email, Long videoId) {
        User user = serviceUtils.getUserByEmailOrThrow(email);

        Video video = serviceUtils.getVideoByIdOrThrow(videoId);

        user.addToWatchlist(video);
        userRepository.save(user);
        return new MessageResponse("Video added to Watchlist");
    }

    @Override
    public MessageResponse removeFromWatchlist(String email, Long videoId) {
        User user = serviceUtils.getUserByEmailOrThrow(email);

        Video video = serviceUtils.getVideoByIdOrThrow(videoId);

        user.removeFromWatchlist(video);
        userRepository.save(user);

        return new MessageResponse("Video removed from Watchlist");
    }

    @Override
    public PageResponse<VideoResponse> getWatchlistPaginated(String email, int page, int size, String search) {

        User user =  serviceUtils.getUserByEmailOrThrow(email);

        Pageable pageable = PaginationUtils.createPageRequest(page, size);
        Page<Video> videoPage;

        if(search != null && !search.trim().isEmpty()) {
            videoPage = userRepository.searchWatchlistByUserId(user.getId(), search.trim(), pageable);
        } else {
            videoPage = userRepository.findWatchlistByUserId(user.getId(), pageable);
        }

        return PaginationUtils.topageResponse(videoPage, VideoResponse::fromEntity);
    }
}
