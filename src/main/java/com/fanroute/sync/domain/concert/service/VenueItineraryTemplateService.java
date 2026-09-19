package com.fanroute.sync.domain.concert.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.concert.dto.VenueItineraryTemplateDto;
import com.fanroute.sync.domain.concert.entity.Venue;
import com.fanroute.sync.domain.concert.entity.VenueItineraryTemplate;
import com.fanroute.sync.domain.concert.entity.VenueItineraryTemplateItem;
import com.fanroute.sync.domain.concert.exception.ConcertErrorCode;
import com.fanroute.sync.domain.concert.repository.VenueItineraryTemplateItemRepository;
import com.fanroute.sync.domain.concert.repository.VenueItineraryTemplateRepository;
import com.fanroute.sync.domain.concert.repository.VenueRepository;
import com.fanroute.sync.domain.place.service.PlaceService;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueItineraryTemplateService {

  private final VenueItineraryTemplateRepository templateRepository;
  private final VenueItineraryTemplateItemRepository itemRepository;
  private final VenueRepository venueRepository;
  private final PlaceService placeService;

  public List<VenueItineraryTemplateDto.Response> getTemplates(Long venueId) {
    if (!venueRepository.existsById(venueId)) {
      throw new BusinessException(ConcertErrorCode.VENUE_NOT_FOUND);
    }
    return templateRepository.findByVenueIdOrderByNameAsc(venueId).stream()
        .map(template -> VenueItineraryTemplateDto.Response.from(template,
            itemRepository.findByTemplateIdOrderBySortOrderAsc(template.getId())))
        .toList();
  }

  public VenueItineraryTemplate getTemplate(Long templateId) {
    return templateRepository.findById(templateId)
        .orElseThrow(() -> new BusinessException(ConcertErrorCode.ITINERARY_TEMPLATE_NOT_FOUND));
  }

  public List<VenueItineraryTemplateItem> getItems(Long templateId) {
    return itemRepository.findByTemplateIdOrderBySortOrderAsc(templateId);
  }

  @Transactional
  public VenueItineraryTemplateDto.Response importTemplate(
      VenueItineraryTemplateDto.ImportRequest request) {
    Venue venue = venueRepository.findById(request.venueId())
        .orElseThrow(() -> new BusinessException(ConcertErrorCode.VENUE_NOT_FOUND));
    VenueItineraryTemplate template = templateRepository
        .findByVenueIdAndName(request.venueId(), request.name())
        .orElseGet(() -> templateRepository.save(
            VenueItineraryTemplate.create(venue, request.name(), request.description())));
    template.update(request.description());

    itemRepository.deleteByTemplateId(template.getId());
    itemRepository.flush();
    List<VenueItineraryTemplateItem> items = request.items().stream()
        .map(item -> VenueItineraryTemplateItem.create(template,
            placeService.getPlace(item.placeId()), item.sortOrder(), item.defaultTime(),
            item.defaultDurationMinutes()))
        .toList();
    itemRepository.saveAll(items);
    return VenueItineraryTemplateDto.Response.from(template,
        itemRepository.findByTemplateIdOrderBySortOrderAsc(template.getId()));
  }
}
