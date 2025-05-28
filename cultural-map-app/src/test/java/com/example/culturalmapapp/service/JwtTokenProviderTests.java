package com.example.culturalmapapp.service;

import com.example.culturalmapapp.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JwtTokenProviderTests {

    @Mock
    private JwtConfig jwtConfig;

    private JwtTokenProvider jwtTokenProvider;

    private final String testSecret = "TestSecretKeyForJwtTokenProviderUnitTestsMustBeLongEnoughForHS256";
    private final long testExpirationMs = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        when(jwtConfig.getSecret()).thenReturn(testSecret);
        when(jwtConfig.getExpirationMs()).thenReturn(testExpirationMs);
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
    }

    private Authentication createMockAuthentication(String username, String role) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
        // Need to cast authorities for the mock to work correctly with the generic signature
        when(authentication.getAuthorities()).thenAnswer(invocation -> (Collection) authorities);
        return authentication;
    }

    @Test
    void testGenerateToken_Success() {
        Authentication authentication = createMockAuthentication("testuser", "ROLE_USER");
        String token = jwtTokenProvider.generateToken(authentication);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(testSecret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("testuser", claims.getSubject());
        assertEquals("ROLE_USER", claims.get("roles", String.class));
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void testGetUsernameFromJWT_Success() {
        Authentication authentication = createMockAuthentication("testuser", "ROLE_USER");
        String token = jwtTokenProvider.generateToken(authentication);
        String username = jwtTokenProvider.getUsernameFromJWT(token);
        assertEquals("testuser", username);
    }
    
    @Test
    void testGetClaimsFromJWT_Success() {
        Authentication authentication = createMockAuthentication("testuser", "ROLE_USER");
        String token = jwtTokenProvider.generateToken(authentication);
        Claims claims = jwtTokenProvider.getClaimsFromJWT(token);
        assertNotNull(claims);
        assertEquals("testuser", claims.getSubject());
        assertEquals("ROLE_USER", claims.get("roles", String.class));
    }


    @Test
    void testValidateToken_ValidToken_ReturnsTrue() {
        Authentication authentication = createMockAuthentication("testuser", "ROLE_USER");
        String token = jwtTokenProvider.generateToken(authentication);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void testValidateToken_InvalidSignature_ReturnsFalse() {
        Authentication authentication = createMockAuthentication("testuser", "ROLE_USER");
        String token = jwtTokenProvider.generateToken(authentication);
        // Tamper with the token or use a different key for validation attempt
        String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx"; 
        //This might not always result in SignatureException but rather MalformedJwtException
        //A more reliable way is to sign with a different key for this specific test
        SecretKey otherKey = Keys.hmacShaKeyFor("AnotherSecretKeyForTestingPurposesThatIsSufficientlyLong".getBytes());
        String tokenSignedWithDifferentKey = Jwts.builder()
            .subject("testuser")
            .claim("roles", "ROLE_USER")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + testExpirationMs))
            .signWith(otherKey)
            .compact();

        assertFalse(jwtTokenProvider.validateToken(tokenSignedWithDifferentKey));
    }
    
    @Test
    void testValidateToken_MalformedToken_ReturnsFalse() {
        String malformedToken = "this.is.not.a.jwt";
        assertFalse(jwtTokenProvider.validateToken(malformedToken));
    }


    @Test
    void testValidateToken_ExpiredToken_ReturnsFalse() {
        when(jwtConfig.getExpirationMs()).thenReturn(-testExpirationMs); // Negative expiration for immediate expiry
        jwtTokenProvider = new JwtTokenProvider(jwtConfig); // Re-initialize with new config

        Authentication authentication = createMockAuthentication("testuser", "ROLE_USER");
        String expiredToken = jwtTokenProvider.generateToken(authentication);
        
        // Need to wait a moment for the token to actually be considered expired by the check
        // However, since we set a negative expiration, it should be expired upon creation.
        // If the validation logic relies on current time vs expiry time, this should work.
        // Jwts.parser() internally checks if expiration.before(new Date())
        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }
    
    @Test
    void testValidateToken_UnsupportedToken_ReturnsFalse() {
        // JJWT's builder typically creates compact JWTs (JWS).
        // Creating a JWE or an unsecured JWT (alg: none) would be more involved.
        // For simplicity, this test might be hard to achieve without manually constructing such a token.
        // An empty string or a non-JWS token might trigger MalformedJwtException or IllegalArgumentException.
        String nonJwsToken = "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ0ZXN0In0."; // Unsecured JWT (alg: none)
        // Note: JJWT by default might not allow "none" algorithm unless explicitly configured.
        // This will likely result in MalformedJwtException or SignatureException if "none" is disallowed.
        assertFalse(jwtTokenProvider.validateToken(nonJwsToken));
    }

    @Test
    void testValidateToken_EmptyClaims_ReturnsFalse() {
        // This is tricky to test directly as an empty claim string is usually a malformed token.
        // Jwts.builder() does not allow empty subject without throwing error.
        // An empty or null token string will be caught by StringUtils.hasText in JwtAuthenticationFilter first.
        // If an empty claims string somehow gets through, Jwts.parser will likely throw IllegalArgumentException.
        String tokenWithEmptyClaims = ""; // Or some other invalid structure
        assertFalse(jwtTokenProvider.validateToken(tokenWithEmptyClaims));
    }
}
