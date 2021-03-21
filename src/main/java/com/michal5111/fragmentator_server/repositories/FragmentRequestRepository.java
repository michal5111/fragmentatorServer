package com.michal5111.fragmentator_server.repositories;

import com.michal5111.fragmentator_server.domain.FragmentRequest;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FragmentRequestRepository extends CrudRepository<FragmentRequest, Long> {

    List<FragmentRequest> deleteAllByMovieId(Long movieId);
}
