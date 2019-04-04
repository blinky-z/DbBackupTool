package com.example.demo.configuration;

import com.example.demo.settings.UserSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private UserSettings userSettings;

    @Autowired
    public void setUserConfig(UserSettings userSettings) {
        this.userSettings = userSettings;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().permitAll()

                .and()
                .formLogin().loginPage("/login").loginProcessingUrl("/api/login").permitAll()

                .and()
                .logout().logoutSuccessUrl("/").logoutUrl("/api/logout")

                .and()
                .csrf().disable();
    }

    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        UserDetails user =
                User.builder().username(userSettings.getWebUILogin()).password(passwordEncoder().encode(userSettings.getWebUIPassword()))
                        .roles("USER").build();

        return new InMemoryUserDetailsManager(user);
    }
}
