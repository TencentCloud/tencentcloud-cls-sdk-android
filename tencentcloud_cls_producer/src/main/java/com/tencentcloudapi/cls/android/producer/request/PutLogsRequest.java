package com.tencentcloudapi.cls.android.producer.request;

import com.tencentcloudapi.cls.android.producer.common.Logs;
import com.tencentcloudapi.cls.android.producer.common.Constants;
import com.tencentcloudapi.cls.android.producer.common.Constants.CompressType;


/**
 * The request used to send data to sls server
 * 
 * @author farmerx
 * 
 */
public class PutLogsRequest extends Request {
	private static final long serialVersionUID = 7226856831224917838L;
	private String mTopic;
	private String mSource;
	private Logs.LogGroup.Builder mlogItems;
	private CompressType compressType = CompressType.LZ4;
	private String mContentType = Constants.CONST_PROTO_BUF;
	private String mFilename;

	public String getFilename() {
		return mFilename;
	}

	public void setFilename(String filename) {
		this.mFilename = filename;
	}

	/**
	 * @return the compressType
	 */
	public CompressType GetCompressType() {
		return compressType;
	}

	/**
	 * @param compressType the compressType to set
	 */
	public void SetCompressType(CompressType compressType) {
		this.compressType = compressType;
	}

	public String getContentType() {
		return mContentType;
	}

	public void setContentType(String contentType) {
		this.mContentType = contentType;
	}

	/**
	 * Construct a put log request
	 * 
	 * @param topic topic name of the log store
	 * @param source source of the log
	 * @param logItems log data
	 */
	public PutLogsRequest(String topic, String source, String filename, Logs.LogGroup.Builder logItems) {
		super();
		mTopic = topic;
		mSource = source;
		mFilename = filename;
		mlogItems = logItems;
	}

	/**
	 * Get the topic
	 * 
	 * @return the topic
	 */
	public String GetTopic() {
		return mTopic;
	}

	/**
	 * Set topic value
	 * 
	 * @param topic topic value
	 */
	public void SetTopic(String topic) {
		mTopic = topic;
	}

	/**
	 * Get log source
	 * 
	 * @return log source
	 */
	public String GetSource() {
		return mSource;
	}

	/**
	 * Set log source
	 * 
	 * @param source log source
	 */
	public void SetSource(String source) {
		mSource = source;
	}

	/**
	 * Get all the log data
	 * 
	 * @return log data
	 */
	public Logs.LogGroup.Builder GetLogItems() {
		return mlogItems;
	}

	/**
	 * Get all the logGroupBytes
	 *
	 * @return logGroupBytes
	 */
	public byte[] GetLogGroupBytes(String sourceIp, String PackageId) {
		Logs.LogGroupList.Builder grpList = Logs.LogGroupList.newBuilder();
		this.mlogItems.setFilename(this.mFilename);
		this.mlogItems.setContextFlow(PackageId);

		if (this.mSource == null || this.mSource.isEmpty()) {
			this.mlogItems.setSource(sourceIp);
		} else {
			this.mlogItems.setSource(this.mSource);
		}

		return grpList.addLogGroupList(this.mlogItems).build().toByteArray();
	}

	/**
	 * Set the log data , shallow copy is used to set the log data
	 * 
	 * @param logItems log data
	 */
	public void SetlogItems(Logs.LogGroup.Builder logItems) {
		mlogItems = logItems;
	}
}
