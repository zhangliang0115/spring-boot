/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.buildpack.platform.docker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.buildpack.platform.docker.Http.Response;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link HttpClientHttp}.
 *
 * @author Phillip Webb
 */
class HttpClientHttpTests {

	private static final String APPLICATION_JSON = "application/json";

	@Mock
	private CloseableHttpClient client;

	@Mock
	private CloseableHttpResponse response;

	@Mock
	private StatusLine statusLine;

	@Mock
	private HttpEntity entity;

	@Mock
	private InputStream content;

	@Captor
	private ArgumentCaptor<HttpUriRequest> requestCaptor;

	private HttpClientHttp http;

	private URI uri;

	@BeforeEach
	void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		given(this.client.execute(any())).willReturn(this.response);
		given(this.response.getEntity()).willReturn(this.entity);
		given(this.response.getStatusLine()).willReturn(this.statusLine);
		this.http = new HttpClientHttp(this.client);
		this.uri = new URI("docker://localhost/example");
	}

	@Test
	void getShouldExecuteHttpGet() throws Exception {
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.get(this.uri);
		verify(this.client).execute(this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		assertThat(request).isInstanceOf(HttpGet.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(request.getFirstHeader(HttpHeaders.CONTENT_TYPE)).isNull();
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void postShouldExecuteHttpPost() throws Exception {
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.post(this.uri);
		verify(this.client).execute(this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		assertThat(request).isInstanceOf(HttpPost.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(request.getFirstHeader(HttpHeaders.CONTENT_TYPE)).isNull();
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void postWithContentShouldExecuteHttpPost() throws Exception {
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.post(this.uri, APPLICATION_JSON,
				(out) -> StreamUtils.copy("test", StandardCharsets.UTF_8, out));
		verify(this.client).execute(this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
		assertThat(request).isInstanceOf(HttpPost.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(request.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue()).isEqualTo(APPLICATION_JSON);
		assertThat(entity.isRepeatable()).isFalse();
		assertThat(entity.getContentLength()).isEqualTo(-1);
		assertThat(entity.isStreaming()).isTrue();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> entity.getContent());
		assertThat(writeToString(entity)).isEqualTo("test");
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void putWithContentShouldExecuteHttpPut() throws Exception {
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.put(this.uri, APPLICATION_JSON,
				(out) -> StreamUtils.copy("test", StandardCharsets.UTF_8, out));
		verify(this.client).execute(this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
		assertThat(request).isInstanceOf(HttpPut.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(request.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue()).isEqualTo(APPLICATION_JSON);
		assertThat(entity.isRepeatable()).isFalse();
		assertThat(entity.getContentLength()).isEqualTo(-1);
		assertThat(entity.isStreaming()).isTrue();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> entity.getContent());
		assertThat(writeToString(entity)).isEqualTo("test");
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void deleteShouldExecuteHttpDelete() throws IOException {
		given(this.entity.getContent()).willReturn(this.content);
		given(this.statusLine.getStatusCode()).willReturn(200);
		Response response = this.http.delete(this.uri);
		verify(this.client).execute(this.requestCaptor.capture());
		HttpUriRequest request = this.requestCaptor.getValue();
		assertThat(request).isInstanceOf(HttpDelete.class);
		assertThat(request.getURI()).isEqualTo(this.uri);
		assertThat(request.getFirstHeader(HttpHeaders.CONTENT_TYPE)).isNull();
		assertThat(response.getContent()).isSameAs(this.content);
	}

	@Test
	void executeWhenResposeIsIn400RangeShouldThrowDockerException() throws ClientProtocolException, IOException {
		given(this.entity.getContent()).willReturn(getClass().getResourceAsStream("errors.json"));
		given(this.statusLine.getStatusCode()).willReturn(404);
		assertThatExceptionOfType(DockerException.class).isThrownBy(() -> this.http.get(this.uri))
				.satisfies((ex) -> assertThat(ex.getErrors()).hasSize(2));
	}

	@Test
	void executeWhenResposeIsIn500RangeShouldThrowDockerException() throws ClientProtocolException, IOException {
		given(this.statusLine.getStatusCode()).willReturn(500);
		assertThatExceptionOfType(DockerException.class).isThrownBy(() -> this.http.get(this.uri))
				.satisfies((ex) -> assertThat(ex.getErrors()).isNull());
	}

	private String writeToString(HttpEntity entity) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		entity.writeTo(out);
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}

}
