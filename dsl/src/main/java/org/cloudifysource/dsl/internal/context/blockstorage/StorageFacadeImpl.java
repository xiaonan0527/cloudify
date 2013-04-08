/*******************************************************************************
 * Copyright (c) 2012 GigaSpaces Technologies Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.cloudifysource.dsl.internal.context.blockstorage;

import com.gigaspaces.client.ChangeSet;
import org.cloudifysource.dsl.cloud.storage.ServiceVolume;
import org.cloudifysource.dsl.cloud.storage.StorageTemplate;
import org.cloudifysource.dsl.cloud.storage.VolumeState;
import org.cloudifysource.dsl.context.ServiceContext;
import org.cloudifysource.dsl.context.blockstorage.LocalStorageOperationException;
import org.cloudifysource.dsl.context.blockstorage.RemoteStorageOperationException;
import org.cloudifysource.dsl.context.blockstorage.StorageFacade;
import org.cloudifysource.dsl.context.kvstorage.AttributesFacadeImpl;
import org.cloudifysource.dsl.context.utils.VolumeUtils;
import org.cloudifysource.dsl.internal.context.RemoteStorageProvisioningDriver;
import org.openspaces.core.GigaSpace;

import java.util.concurrent.TimeoutException;

/**
 * 
 * @author elip
 *
 */
public class StorageFacadeImpl implements StorageFacade {

	private static final long AFTER_ATTACH_TIMEOUT = 10 * 1000;
	
	private ServiceContext serviceContext;
	private RemoteStorageProvisioningDriver remoteStorageProvisioningDriver;
    private GigaSpace managementSpace;
	
	
	public StorageFacadeImpl(final ServiceContext serviceContext,
			final RemoteStorageProvisioningDriver storageApi) {
		this.serviceContext = serviceContext;
		this.remoteStorageProvisioningDriver = storageApi;
        this.managementSpace = ((AttributesFacadeImpl) serviceContext.getAttributes()).getManagementSpace();
	}

    @Override
	public void attachVolume(final String volumeId, final String device) 
					throws RemoteStorageOperationException, LocalStorageOperationException {
		validateNotWindows();
		remoteStorageProvisioningDriver.attachVolume(volumeId, device, serviceContext.getBindAddress());
        changeStateOfVolumeWithId(volumeId, VolumeState.ATTACHED);
        setDeviceForVolumeWithId(volumeId, device);
		try {
			// ugly as hell. can't really figure out how to wait here properly for the volume to actually be attached.
			// see CLOUDIFY-1551
			Thread.sleep(AFTER_ATTACH_TIMEOUT);
		} catch (InterruptedException e) {
			throw new LocalStorageOperationException(e);
		}
	}

    @Override
	public String createVolume(final String templateName) throws RemoteStorageOperationException, TimeoutException {
        String volumeId = remoteStorageProvisioningDriver.createVolume(templateName, serviceContext.getLocationId());
        writeNewVolume(volumeId);
        return volumeId;
	}

    @Override
	public String createVolume(final String templateName, final long timeoutInMillis)
			throws RemoteStorageOperationException, TimeoutException {
        String volumeId = remoteStorageProvisioningDriver.
                createVolume(templateName, serviceContext.getLocationId(), timeoutInMillis);
        writeNewVolume(volumeId);
        return volumeId;
	}

	@Override
	public void detachVolume(final String volumeId) 
			throws RemoteStorageOperationException, LocalStorageOperationException {
		validateNotWindows();
		remoteStorageProvisioningDriver.detachVolume(volumeId, serviceContext.getBindAddress());
        changeStateOfVolumeWithId(volumeId, VolumeState.DETACHED);
	}

	@Override
	public void deleteVolume(final String volumeId) throws RemoteStorageOperationException {
		validateNotWindows();
		remoteStorageProvisioningDriver.deleteVolume(serviceContext.getLocationId(), volumeId);
        deleteVolumeWithId(volumeId);
	}

    @Override
	public void mount(final String device, final String path, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot mount when not running in privileged mode");
		}
		VolumeUtils.mount(device, path, timeoutInMillis);
        changeStateOfVolumeWithDevice(device, VolumeState.MOUNTED);
		
	}

	@Override
	public void mount(final String device, final String path) 
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot mount when not running in privileged mode");
		}
		VolumeUtils.mount(device, path);
        changeStateOfVolumeWithDevice(device, VolumeState.MOUNTED);
	}

	@Override
	public void unmount(final String device, final long timeoutInMillis) 
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot unmount when not running in privileged mode");
		}
		VolumeUtils.unmount(device, timeoutInMillis);
        changeStateOfVolumeWithDevice(device, VolumeState.UNMOUNTED);
	}

	@Override
	public void unmount(final String device) 
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot unmount when not running in privileged mode");
		}
		VolumeUtils.unmount(device);
        changeStateOfVolumeWithDevice(device, VolumeState.UNMOUNTED);
	}

    @Override
    public StorageTemplate getTemplate(final String templateName) {
        return remoteStorageProvisioningDriver.getTemplate(templateName);
    }

    @Override
	public void format(final String device, final String fileSystem, final long timeoutInMillis)
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot format when not running in privileged mode");
		}
		VolumeUtils.format(device, fileSystem, timeoutInMillis);
        changeStateOfVolumeWithDevice(device, VolumeState.FORMATTED);
		
	}

	@Override
	public void format(final String device, final String fileSystem)
			throws LocalStorageOperationException, TimeoutException {
		validateNotWindows();
		if (!serviceContext.isPrivileged()) {
			throw new IllegalStateException("Cannot format when not running in privileged mode");
		}
		VolumeUtils.format(device, fileSystem);
        changeStateOfVolumeWithDevice(device, VolumeState.FORMATTED);
	}

    private void validateNotWindows() {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            throw new UnsupportedOperationException("Windows OS is not supported for Storage API");
        }
    }

    private void deleteVolumeWithId(final String volumeId) {
        managementSpace.takeById(ServiceVolume.class, volumeId);
    }

    private void writeNewVolume(final String volumeId) {
        ServiceVolume serviceVolume = new ServiceVolume(serviceContext.getApplicationName(), serviceContext.getServiceName());
        serviceVolume.setId(volumeId);
        serviceVolume.setState(VolumeState.CREATED);
        managementSpace.write(serviceVolume);
    }

    private void changeStateOfVolumeWithId(final String volumeId, final VolumeState newState) {
        ServiceVolume serviceVolume = new ServiceVolume(serviceContext.getApplicationName(), serviceContext.getServiceName());
        serviceVolume.setId(volumeId);
        managementSpace.change(serviceVolume, new ChangeSet().set("state", newState));
    }

    private void changeStateOfVolumeWithDevice(final String deviceName, final VolumeState newState) {
        ServiceVolume serviceVolume = new ServiceVolume(serviceContext.getApplicationName(), serviceContext.getServiceName());
        serviceVolume.setDevice(deviceName);
        managementSpace.change(serviceVolume, new ChangeSet().set("state", newState));
    }

    private void setDeviceForVolumeWithId(final String volumeId, final String device) {
        ServiceVolume serviceVolume = new ServiceVolume(serviceContext.getApplicationName(), serviceContext.getServiceName());
        serviceVolume.setId(volumeId);
        managementSpace.change(serviceVolume, new ChangeSet().set("device", device));
    }

}
