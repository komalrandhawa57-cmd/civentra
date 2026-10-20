package com.civentra.web;

import com.civentra.dao.IssueDAO;
import com.civentra.dao.UserDAO;
import com.civentra.model.User;
import com.civentra.service.ImageStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.Locale;

@RestController
public class EvidenceController {
    private final ImageStorageService imageStorageService;
    private final IssueDAO issueDAO;
    private final UserDAO userDAO;

    public EvidenceController(ImageStorageService imageStorageService, IssueDAO issueDAO, UserDAO userDAO) {
        this.imageStorageService = imageStorageService;
        this.issueDAO = issueDAO;
        this.userDAO = userDAO;
    }

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> getEvidence(@PathVariable String filename, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("civentra.user");
        if (!(value instanceof User sessionUser) || !sessionUser.isActive()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        User user;
        try {
            user = userDAO.findById(sessionUser.getUserId());
        } catch (java.sql.SQLException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (user == null || !user.isActive()) {
            session.removeAttribute("civentra.user");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        session.setAttribute("civentra.user", user);

        Path image = imageStorageService.findStoredImage(filename).orElse(null);
        if (image == null) return ResponseEntity.notFound().build();

        String evidencePath = "/uploads/" + filename;
        boolean allowed = "ADMIN".equalsIgnoreCase(user.getRoleName())
                || issueDAO.canViewEvidence(evidencePath, user.getUserId());
        if (!allowed) return ResponseEntity.notFound().build();

        MediaType mediaType = switch (filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)) {
            case "png" -> MediaType.IMAGE_PNG;
            case "jpg" -> MediaType.IMAGE_JPEG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(new FileSystemResource(image));
    }
}
