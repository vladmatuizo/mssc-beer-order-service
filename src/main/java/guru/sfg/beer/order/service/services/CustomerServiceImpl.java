package guru.sfg.beer.order.service.services;

import com.example.common.model.CustomerPagedList;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.web.mappers.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    public CustomerPagedList listCustomers(Pageable pageable) {
        Page<Customer> customerPage = customerRepository.findAll(pageable);
        Pageable customersPageable = customerPage.getPageable();

        return new CustomerPagedList(
                customerPage.stream()
                        .map(customerMapper::customerToDto)
                        .collect(Collectors.toList()),
                PageRequest.of(customersPageable.getPageNumber(), customersPageable.getPageSize()),
                customerPage.getTotalElements());
    }
}
