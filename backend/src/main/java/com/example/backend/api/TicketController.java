package com.example.backend.api;

import com.example.backend.model.PageResult;
import com.example.backend.model.Priority;
import com.example.backend.model.TicketStatus;
import com.example.backend.service.TicketService;
import com.example.backend.service.TicketService.TicketView;
import com.example.backend.service.TicketService.TicketFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Ticket")
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService tickets;

    public TicketController(TicketService tickets) {
        this.tickets = tickets;
    }

    public record CreateTicket(@NotBlank @Size(max = 200)
                               @Schema(description = "Không được để trống hoặc chỉ chứa khoảng trắng", example = "Máy in không hoạt động") String title,
                               @NotBlank @Schema(example = "Máy in phòng A không nhận lệnh") String description,
                               @Schema(description = "Bỏ qua thì dùng MEDIUM", example = "MEDIUM") Priority priority,
                               @NotNull @Schema(description = "ID danh mục lấy từ GET /api/categories", example = "1") Long categoryId) { }

    public record UpdateTicket(String title, String description, Priority priority, Long categoryId) { }

    public record StatusChange(@NotNull TicketStatus status) { }

    public record AssignRequest(@NotNull Long agentId) { }



    @Operation(summary = "Tạo ticket hỗ trợ IT", description = "Người tạo được lấy từ tài khoản đăng nhập. Ticket mới có trạng thái NEW.",
            responses = {
                    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
                    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
                    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketView create(@Valid @RequestBody CreateTicket r) {
        return tickets.create(r.title(), r.description(), r.priority(), r.categoryId(), CurrentUser.get());
    }

    @Operation(summary = "Lọc và phân trang danh sách ticket", description = "USER chỉ xem ticket của mình; AGENT và ADMIN xem tất cả. page bắt đầu từ 0, size mặc định 20.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Trang danh sách ticket"),
                    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
                    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")})
    @GetMapping
    public PageResult<TicketView> list(@RequestParam(required = false) TicketStatus status,
                                       @RequestParam(required = false) Priority priority,
                                       @RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false) Long agentId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        TicketFilter filter = new TicketFilter(status, priority, categoryId, agentId);
        return tickets.list(filter, page, size, CurrentUser.get());
    }

    @Operation(summary = "Xem chi tiết ticket", description = "USER chỉ được xem ticket của mình.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Thông tin ticket"),
                    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
                    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
                    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
                    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    @GetMapping("/{id}")
    public TicketView get(@PathVariable Long id) {
        return tickets.get(id, CurrentUser.get());
    }

    @Operation(summary = "Cập nhật thông tin ticket", description = "PUT và PATCH đều chỉ sửa các trường khác null. USER chỉ sửa ticket của mình khi còn NEW; AGENT chỉ sửa ticket được giao; ADMIN được sửa mọi ticket.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ticket sau khi cập nhật"),
                    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
                    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
                    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
                    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    @RequestMapping(path = "/{id}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public TicketView update(@PathVariable Long id, @RequestBody UpdateTicket r) {
        return tickets.update(id, r.title(), r.description(), r.priority(), r.categoryId(), CurrentUser.get());
    }


    @Operation(summary = "Đổi trạng thái ticket", description = "Luồng: NEW → IN_PROGRESS → RESOLVED → CLOSED; RESOLVED có thể quay lại IN_PROGRESS. USER chỉ đóng ticket RESOLVED của mình; AGENT chỉ xử lý ticket được giao; ADMIN vẫn phải theo luồng trạng thái.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ticket sau khi đổi trạng thái"),
                    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
                    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
                    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
                    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    @PatchMapping("/{id}/status")
    public TicketView changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChange r) {
        return tickets.changeStatus(id, r.status(), CurrentUser.get());
    }


    @Operation(summary = "Admin phân công ticket cho agent", description = "agentId phải thuộc tài khoản có vai trò AGENT.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ticket sau khi phân công"),
                    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
                    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
                    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
                    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    @PatchMapping("/{id}/assign")
    public TicketView assign(@PathVariable Long id, @Valid @RequestBody AssignRequest r) {
        return tickets.assign(id, r.agentId(), CurrentUser.get());
    }


    @Operation(summary = "Xóa ticket", description = "USER chỉ xóa ticket NEW của mình; AGENT không được xóa; ADMIN được xóa mọi ticket. Lịch sử ticket cũng bị xóa.",
            responses = {
                    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
                    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
                    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
                    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tickets.delete(id, CurrentUser.get());
    }


    @Operation(summary = "Xem lịch sử thay đổi ticket", description = "USER chỉ xem lịch sử ticket của mình.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Danh sách lịch sử thay đổi ticket"),
                    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
                    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
                    @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
                    @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")})
    @GetMapping("/{id}/history")
    public List<TicketService.HistoryView> history(@PathVariable Long id) {
        return tickets.history(id, CurrentUser.get());
    }

}
