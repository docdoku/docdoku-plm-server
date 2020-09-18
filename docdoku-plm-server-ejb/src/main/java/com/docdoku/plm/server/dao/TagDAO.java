/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2020 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.docdoku.plm.server.dao;

import com.docdoku.plm.server.core.exceptions.CreationException;
import com.docdoku.plm.server.core.exceptions.TagAlreadyExistsException;
import com.docdoku.plm.server.core.exceptions.TagNotFoundException;
import com.docdoku.plm.server.core.meta.Tag;
import com.docdoku.plm.server.core.meta.TagKey;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import java.util.List;


@RequestScoped
public class TagDAO {

    @Inject
    private EntityManager em;

    public TagDAO() {
    }

    public Tag[] findAllTags(String pWorkspaceId) {
        Tag[] tags;
        TypedQuery<Tag> query = em.createQuery("SELECT DISTINCT t FROM Tag t WHERE t.workspaceId = :workspaceId",Tag.class);
        List<Tag> listTags = query.setParameter("workspaceId", pWorkspaceId).getResultList();
        tags = new Tag[listTags.size()];
        for (int i = 0; i < listTags.size(); i++) {
            tags[i] = listTags.get(i);
        }

        return tags;
    }

    public void removeTag(TagKey pTagKey) throws TagNotFoundException {
        Tag tag = em.find(Tag.class, pTagKey);
        if (tag == null) {
            throw new TagNotFoundException(pTagKey);
        } else {
            em.remove(tag);
        }
    }


    public Tag loadTag(TagKey pTagKey) throws TagNotFoundException {
        Tag tag = em.find(Tag.class,pTagKey);
        if (tag == null) {
            throw new TagNotFoundException(pTagKey);
        } else {
            return tag;
        }
    }

    public void createTag(Tag pTag) throws CreationException, TagAlreadyExistsException {
        createTag(pTag, false);
    }

    public void createTag(Tag pTag, boolean silent) throws CreationException, TagAlreadyExistsException {
        try {
            //the EntityExistsException is thrown only when flush occurs
            em.persist(pTag);
            em.flush();
        } catch (EntityExistsException pEEEx) {
            if(!silent) {
                throw new TagAlreadyExistsException(pTag);
            }
        } catch (PersistenceException pPEx) {
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            if(!silent) {
                throw new CreationException();
            }
        }
    }
}
