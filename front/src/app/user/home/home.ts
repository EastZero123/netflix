import {ChangeDetectorRef, Component, HostListener, OnDestroy, OnInit} from '@angular/core';
import {debounceTime, distinctUntilChanged, filter, Subject} from 'rxjs';
import {VideoService} from '../../shared/services/video-service';
import {WatchlistService} from '../../shared/services/watchlist-service';
import {NotificationService} from '../../shared/services/notification-service';
import {UtilityService} from '../../shared/services/utility-service';
import {MediaService} from '../../shared/services/media-service';
import {DialogService} from '../../shared/services/dialog-service';
import {ErrorHandlerService} from '../../shared/services/error-handler-service';

@Component({
  selector: 'app-home',
  standalone: false,
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit, OnDestroy {
  allVideos: any = [];
  filteredVideos: any = [];
  loading = true;
  loadingMore = false;
  error = false;
  searchQuery: string = '';

  featuredVideos:any[] = [];
  currentSlideIndex = 0;
  featuredLoading = true;

  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  hasMoreVideos = true;

  private searchSubject = new Subject<String>();
  private sliderInterval:any;
  private savedScrollPosition: number = 0;

  constructor(
    private videoService: VideoService,
    private watchlistService: WatchlistService,
    private notification: NotificationService,
    public utilityService: UtilityService,
    public mediaService: MediaService,
    private dialogService: DialogService,
    private cdr: ChangeDetectorRef,
    private errorHandlerService: ErrorHandlerService,
  ) {
  }

  ngOnInit():void {
    this.loadFeaturedVideos()
    this.loadVideos()
    this.initializeSearchDebounce()
  }

  ngOnDestroy():void {
    this.searchSubject.complete();
    this.stopSlider();
  }

  initializeSearchDebounce():void {
    this.searchSubject.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(() => {
      this.performSearch()
    })
  }

  loadFeaturedVideos() {
    this.featuredLoading = true;
    this.videoService.getFeaturedVideos().subscribe({
      next: (videos:any) => {
        this.featuredVideos = videos;
        this.featuredLoading = false;
        if(this.featuredVideos.length > 1) {
          this.startSlider();
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.featuredLoading = false;
        this.errorHandlerService.handle(err, 'Error loading featuredVideos');
      }
    })
  }

  private startSlider() {
    this.sliderInterval = setInterval(() => {
      this.nextSlide()
    }, 5000)
  }

  private stopSlider() {
    if(this.sliderInterval) {
      clearInterval(this.sliderInterval);
    }
  }

  nextSlide() {
    if(this.featuredVideos.length > 0) {
      this.currentSlideIndex = (this.currentSlideIndex + 1) % this.featuredVideos.length;
    }
  }

  prevSlide() {
    if(this.featuredVideos.length > 0) {
      this.currentSlideIndex = (this.currentSlideIndex - 1 + this.featuredVideos.length) % this.featuredVideos.length;
    }
  }

  goToSlide(index:number) {
    this.currentSlideIndex = index;
    this.stopSlider();
    if(this.featuredVideos.length > 1) {
      this.startSlider()
    }
  }

  getCurrentFeaturedVideo() {
    return this.featuredVideos[this.currentSlideIndex] || null;
  }

  @HostListener('window:scroll')
  OnScroll():void {
    const scrollPosition = window.pageYOffset + window.innerHeight;
    const pageHeight = document.documentElement.scrollHeight;

    if(scrollPosition >= pageHeight - 200 && !this.loadingMore && !this.loading && this.hasMoreVideos) {
      this.loadMoreVideos()
    }
  }

  loadVideos(page:number = 0) {
    this.error = false;
    this.currentPage = 0;
    this.allVideos = [];
    this.filteredVideos = [];
    const search = this.searchQuery.trim() || undefined;
    const isSearching = !!search;
    this.loading = true;

    this.videoService.getPublishedVideosPaginated(page, this.pageSize, search).subscribe({
      next: (response:any) => {
        this.allVideos = response.content;
        this.filteredVideos = response.content;
        this.currentPage = response.number;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.hasMoreVideos = this.currentPage < this.totalPages - 1;
        this.loading = false;

        if(isSearching && this.savedScrollPosition > 0) {
          setTimeout(() => {
            window.scrollTo({
              top: this.savedScrollPosition,
              behavior: 'auto'
            });
            this.savedScrollPosition = 0;
          }, 0);
        }

        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading videos page', err);
        this.error = true;
        this.loading = false;
        this.savedScrollPosition = 0;
      }
    })
  }

  loadMoreVideos() {
    if(this.loadingMore || !this.hasMoreVideos) {
      return;
    }
    this.loadingMore = true;
    const nextPage = this.currentPage + 1;
    const search = this.searchQuery.trim() || undefined;

    this.videoService.getPublishedVideosPaginated(nextPage, this.pageSize, search).subscribe({
      next: (response:any) => {
        this.allVideos = [...this.allVideos, ...response.content];
        this.filteredVideos = [...this.filteredVideos, ...response.content];
        this.currentPage = response.number;
        this.hasMoreVideos = this.currentPage < this.totalPages - 1;
        this.loadingMore = false;

        this.cdr.detectChanges();
      },
      error: (err) => {
        this.notification.error('Failed to load more videos');
        this.loadingMore = false;
      }
    })
  }

  onSearch() {
    this.searchSubject.next(this.searchQuery);
  }

  private performSearch() {
    this.savedScrollPosition = window.pageYOffset || document.documentElement.scrollTop;
    this.currentPage = 0;
    this.loadVideos();
  }

  clearSearch() {
    this.searchQuery = '';
    this.currentPage = 0;
    this.savedScrollPosition = 0;
    this.loadVideos();
  }

  isInWatchlist(video:any):boolean {
    return video.isInWatchlist === true;
  }

  toggleWatchlist(video:any, event?:Event) {
    if(event) {
      event.stopPropagation();
    }

    const videoId = video.id;
    const isInList = video.isInWatchlist(video);

    if(isInList) {
      video.isInWatchlist = false;
      this.watchlistService.removeFromWatchlist(videoId).subscribe({
        next: () => {
          this.notification.success('Removing watchlist');

          this.cdr.detectChanges();
        },
        error: (err) => {
          video.isInWatchlist = true;
          this.errorHandlerService.handle(err, 'Error removing watchlist');
        }
      });
    }
    else {
      video.isInWatchlist = true;
      this.watchlistService.addToWatchlist(videoId).subscribe({
        next: () => {
          this.notification.success('Adding watchlist');

          this.cdr.detectChanges();
        },
        error: (err) => {
          video.isInWatchlist = false;
          this.errorHandlerService.handle(err, 'Error Adding watchlist');
        }
      });
    }
  }

  getPosterUrl(video:any) {
    return this.mediaService.getMediaUrl(video, 'image', {
      useCache: true
    }) || '';
  }

  playVideo(video:any) {
    this.dialogService.openVideoPlayer(video);
  }

  formatDuration(seconds:number | undefined):string {
    return this.utilityService.formatDuration(seconds);
  }
}
