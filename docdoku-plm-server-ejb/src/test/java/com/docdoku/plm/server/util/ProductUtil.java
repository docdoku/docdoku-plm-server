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

package com.docdoku.plm.server.util;

import com.docdoku.plm.server.core.common.Account;
import com.docdoku.plm.server.core.common.User;
import com.docdoku.plm.server.core.common.Workspace;
import com.docdoku.plm.server.core.product.*;
import com.docdoku.plm.server.core.util.AlphanumericComparator;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Asmae CHADID on 02/03/15, Ludovic BAREL on 10/18.
 *
 */
public class ProductUtil {


    public static final String WORKSPACE_ID = "TestWorkspace";
    public static final String VERSION = "A";
    public static final int ITERATION = 1;
    public static final String PART_ID = "TestPart";
    public static final String USER_2_NAME = "user2";
    public static final String USER_2_LOGIN = "user2";
    public static final String USER_1_LOGIN = "user1";

    public static final String USER_1_MAIL = "user1@docdoku.com";
    public static final String USER_1_LANGUAGE = "fr";
    public static final String USER_2_MAIL = "user2@docdoku.com";
    public static final String USER_2_LANGUAGE = "en";


    //BEGIN : DEFAULT PARTS
    /**  createTestableParts method will generate a default structure for each part set as follow :
     *
     *      |--> part1
     *             |
     *             |--> part7
     *             |
     *             |--> part8
     *
     *      |--> part2
     *             |
     *             |--> part9
     *             |
     *             |--> part10
     *
     *      |--> part3
     *             |
     *             |--> part11
     *                     |
     *                     |--> part14
     *
     *      |--> part4
     *
     *      |--> part5
     *             |
     *             |--> part12
     *                     |
     *                     |--> part15
     *                             |
     *                             |--> part17
     *
     *      |--> part6
     *             |
     *             |--> part13
     *                     |
     *                     |--> part16
     *                             |
     *                             |--> part18
     *                                     |
     *                                     |--> pPart19
     *                                             |
     *                                             |--> part20
     */

    public final static String[] defaultPartsNumber_list = {

            "PART-001","PART-002","PART-003","PART-004","PART-005",
            "PART-006","PART-007","PART-008","PART-009","PART-010",
            "PART-011","PART-012","PART-013","PART-014","PART-015",
            "PART-016","PART-017","PART-018","PART-019","PART-020"
    };

    /*
     *   Mapping rule  :  defaultMappingUsageLink_tab [i][i] => PART-XYZ related parts
     *   example :
     *
     *           |--> part1
     *                   |
     *                   |--> part7
     *                   |
     *                   |--> part8
     *
     *      so we have :
     *
     *          defaultMappingUsageLink_tab [0] => PART-001
     *          defaultMappingUsageLink_tab [0][0] = {6,7}
     *
     *      with :
     *
     *          defaultPartsNumber_list[6] => PART-007
     *          defaultPartsNumber_list[7] => PART-008
     *
     *     When PART-XYZ have no related the array will be empty
     */
    private final static int [][] defaultMappingUsageLink_tab = {

            {6,7}, {8,9}, {10}, {}, {11},//related to : part1, part2, part3, part4, part5
            {12}, {}, {}, {}, {},     //related to    : part6, part7, part8, part9, part10
            {13}, {14}, {15}, {}, {16},//related to   : part11, part12, part13, part14, part15
            {17}, {}, {18}, {19}, {}//related to      : part16, part17, part18, part19, part20
    };

    private static Workspace workspace;
    public static User user = new User();
    private static List<String> createdNumber_list = new ArrayList<>();
    static List<PartMaster> existingPart_list = new ArrayList<>();
    private static Comparator<CharSequence> STRING_COMPARATOR = new AlphanumericComparator();
    private static HashMap<String,HashMap<String,List<PartRevision>>> metaData_list_revision= new HashMap<>();
    private static HashMap<String,HashMap<String,List<PartIteration>>> metaData_list_iteration = new HashMap<>();
    private static HashMap<String,HashMap<String,PartMaster>> metaData_list_master = new HashMap<>();
    private static List<String> rootsPartNumberInBuild = new ArrayList<>();

    private  static final String PARTMASTER_MAP_KEY = "PartMaster";
    private  static final String REVISIONS_MAP_KEY = "Revisions";
    private  static final String ITERATION_MAP_KEY = "Iterations";

    public static void createTestableParts() throws Exception {

        //in case a creation was done before
        createdNumber_list.clear();
        existingPart_list.clear();

        workspace = new Workspace();
        Account account = new Account(USER_1_LOGIN,USER_1_LOGIN,USER_1_MAIL,"en",new Date(),"GMT");
        user.setWorkspace(workspace);
        user.setAccount(account);

        workspace.setEnabled(true);
        workspace.setFolderLocked(false);
        workspace.setId(WORKSPACE_ID);
        workspace.setAdmin(account);
        for(String partNumber : defaultPartsNumber_list){

            boolean isCreated = (createdNumber_list.contains(partNumber));
            if(!isCreated){

                existingPart_list.add(buildPartMaster(partNumber));
            }
            rootsPartNumberInBuild.clear();
        }
        sort(existingPart_list,existingPart_list.size());
        createMetaData(existingPart_list.size());
    }

    public static PartMaster getPartMasterWith(String partNumber){

        return metaData_list_master.get(partNumber).get(PARTMASTER_MAP_KEY);
    }

    static  List<PartRevision> getPartRevisionsOf(String partNumber){

        return metaData_list_revision.get(partNumber).get(REVISIONS_MAP_KEY);
    }

    private static  void setPartIterationsOf(String partNumber, List<PartIteration> newList){
        metaData_list_iteration.computeIfAbsent(partNumber, k -> new HashMap<>());
        metaData_list_iteration.get(partNumber).put(ITERATION_MAP_KEY,newList);
    }

    private static void setPartRevisionsOf(String partNumber, List<PartRevision> newList){
        metaData_list_revision.computeIfAbsent(partNumber, k -> new HashMap<>());
        metaData_list_revision.get(partNumber).put(REVISIONS_MAP_KEY,newList);
    }

    static  List<PartIteration> getPartIterationsOf(String partNumber){

        return metaData_list_iteration.get(partNumber).get(ITERATION_MAP_KEY);
    }

    static PartRevision getPartRevisionWith(String version, String partNumber){

        return getRevisionWith(version, partNumber,metaData_list_revision.get(partNumber).get(REVISIONS_MAP_KEY).size());
    }

    public static void addRevisionToPartWith(String partNumber, PartRevision revision, boolean checedkout){


        if(checedkout){

            revision.setCheckOutUser(new User());//for metadata
        }
        //avoid double
        if(getRevisionWith(revision.getVersion(),partNumber,
                metaData_list_revision.get(partNumber).get(REVISIONS_MAP_KEY).size()) != null){

            return;
        }

        (metaData_list_revision.get(partNumber).get(REVISIONS_MAP_KEY)).add(revision);
    }

    public static void addRevisionWithPartLinkTo(PartMaster partMaster, String[] usageMembers, boolean released, boolean checkout){

        PartRevision partRevision = partMaster.createNextRevision(user);
        PartIteration partIteration = partRevision.createNextIteration(user);
        partIteration.setCheckInDate(new Date());
        partIteration.setCreationDate(new Date());
        List<PartUsageLink> usage = new ArrayList<>();
        for(String member : usageMembers){

            PartUsageLink partUsageLink=  new PartUsageLink();
            partUsageLink.setComponent(getPartMasterWith(member));
            partUsageLink.setOptional(false);
            partUsageLink.setAmount(1);
            partUsageLink.setReferenceDescription(member + "-UsageLink");
            usage.add(partUsageLink);
        }
        partIteration.setComponents(usage);

        if(released){

            partRevision.release(user);
        }

        //update meta data
        addIterationTo(partMaster.getNumber(), partIteration);
        addRevisionToPartWith(partMaster.getNumber(), partRevision, checkout);
    }

    public static void addIterationTo(String partNumber, PartIteration iteration){

        if(!exist(iteration.getIteration(),partNumber)){

            iteration.setComponents(getPartMasterWith(partNumber)
                    .getLastRevision().getLastIteration()
                    .getComponents());
            metaData_list_iteration.get(partNumber).get(ITERATION_MAP_KEY).add(iteration);
        }
    }

    public static void addSubstituteInLastIterationOfLastRevisionTo(PartMaster partMaster, String[] substituteTab, String toPartNumber){

        List<PartSubstituteLink> substituteLinks = new ArrayList<>();
        for(PartUsageLink partUsageLink : partMaster.getLastRevision().getLastIteration().getComponents()){

            if( partUsageLink.getComponent() == null){
                continue;
            }
            if(partUsageLink.getComponent().getNumber().equals(toPartNumber)){

                for (String aSubstituteTab : substituteTab) {
                    PartSubstituteLink partSubstituteLink = new PartSubstituteLink();
                    partSubstituteLink.setSubstitute(getPartMasterWith(aSubstituteTab));
                    partSubstituteLink.setAmount(1);
                    partSubstituteLink.setReferenceDescription(aSubstituteTab + "-Substitute");
                    substituteLinks.add(partSubstituteLink);
                    partUsageLink.setSubstitutes(substituteLinks);
                    partUsageLink.setOptional(true);
                }

            }
        }

        //update meta data
        setPartIterationsOf(partMaster.getNumber(), partMaster.getLastRevision().getPartIterations());
    }

    public static void areThoseOfRevision(String version, List<PartIteration> inList, String forPartNumber){

        for(PartIteration pI : inList){

            assertEquals(forPartNumber, pI.getPartNumber());
            assertEquals(version, pI.getPartRevision().getVersion());
            assertFalse(pI.getPartRevision().isCheckedOut());
        }
    }

    // warning : you can reuse but do not change implementation may have impact on filters test
    public static void generateSomeReleasedRevisionWithSubstitutesFor(String partNumber){

        PartMaster partMaster = getPartMasterWith(partNumber);

        //Configure members of partMaster
        String[] membersPartMaster = {"PART-006","PART-003","PART-005","PART-008","PART-007"};
        String[] membersPartMasterRevisionK = {"PART-011","PART-012","PART-008","PART-007","PART-016"};

        //configure substitutes of members
        String[] subtitutesForPart011 = {"PART-014"};
        String[] subtitutesForPart012 = {"PART-015"};
        String[] subtitutesForPart016 = {"PART-018","PART-015","PART-009","PART-012"};
        String[] subtitutesForPart007 = {"PART-002","PART-005"};
        String[] subtitutesForPart008 = {"PART-004"};

        // true <=> released
        addRevisionWithPartLinkTo(partMaster, membersPartMaster, true, false);//  revision B
        addRevisionWithPartLinkTo(partMaster, membersPartMaster, false, false);// revision C
        addRevisionWithPartLinkTo(partMaster, membersPartMaster, true, false);//  revision D
        addRevisionWithPartLinkTo(partMaster, membersPartMaster, false, false);// revision E
        addRevisionWithPartLinkTo(partMaster, membersPartMaster, false, false);// revision F
        addRevisionWithPartLinkTo(partMaster, membersPartMaster, true, false);//  revision G
        addRevisionWithPartLinkTo(partMaster, membersPartMaster, true, false);//  revision H
        addRevisionWithPartLinkTo(partMaster, membersPartMaster, false, false);// revision I
        addRevisionWithPartLinkTo(partMaster, membersPartMaster, true, false);//  revision J
        addRevisionWithPartLinkTo(partMaster, membersPartMasterRevisionK, true, false);// revision K

        //Add substitutes to members in revision K
        addSubstituteInLastIterationOfLastRevisionTo(partMaster, subtitutesForPart011, "PART-011");
        addSubstituteInLastIterationOfLastRevisionTo(partMaster, subtitutesForPart012, "PART-012");
        addSubstituteInLastIterationOfLastRevisionTo(partMaster, subtitutesForPart016, "PART-016");
        addSubstituteInLastIterationOfLastRevisionTo(partMaster, subtitutesForPart007, "PART-007");
        addSubstituteInLastIterationOfLastRevisionTo(partMaster, subtitutesForPart008, "PART-008");
    }

    /**
     *
     * BASIC STRUCTURE BUILT : None released but last revision is checked out
     *      Syntax : PART-NUMBER-REVISION-ITERATION
     *
     *  PART-001-A-1 ( Default )
     *      |-> PART-007-A-1
     *      |-> PART-008-A-1
     *
     *  PART-001-B-1
     *      |-> PART-002-B-1
     *              |-> PART-009-A-1
     *              |-> PART-010-A-1
     *              |-> PART-011-A-1
     *      |-> PART-006-B-1
     *              |-> PART-012-A-1
     *                     |-> PART-015
     *              |-> PART-002-B-1
     *                     |-> PART-009-A-1
     *                     |-> PART-010-A-1
     *                     |-> PART-011-A-1
     *              |-> PART-004-A-1
     *      |-> PART-0018-B-1
     *              |-> PART-003-A-1
     *              |-> PART-014-A-1
     *
     *  PART-001-C-1 ( same for PART-001-D-1 )
     *      |-> PART-002-B-1
     *              |-> PART-009-A-1
     *              |-> PART-010-A-1
     *              |-> PART-011-A-1
     *      |-> PART-006-B-1
     *              |-> PART-012-A-1
     *                     |-> PART-015
     *              |-> PART-002-B-1
     *                     |-> PART-009-A-1
     *                     |-> PART-010-A-1
     *                     |-> PART-011-A-1
     *              |-> PART-004-A-1
     *      |-> PART-015-B-1
     *              |-> PART-007-B-1
     *                     |-> PART-009-A-1
     *              |-> PART-008-B-1
     *                      |-> PART-013-A-1
     *                          |--> PART-016-A-1
     *                                  |-->PART-018-A-1
     *                                          |--> PART-019-A-1
     *                                                  |--> PART-020-A-1
     *              |-> PART-004-A-1
     *
     *  PART-001-E-1 ( checked out )
     *      |-> PART-002-B-1
     *              |-> PART-009-A-1
     *              |-> PART-010-A-1
     *              |-> PART-011-A-1
     *      |-> PART-006-B-1
     *              |-> PART-012-A-1
     *                  |-> PART-015-B-1
     *                      |-> PART-007-B-1
     *                          |-> PART-009-A-1
     *                      |-> PART-014-A-1
     *              |-> PART-002-B-1
     *                     |-> PART-009-A-1
     *                     |-> PART-010-A-1
     *                     |-> PART-011-A-1
     *              |-> PART-004-A-1
     *      |-> PART-007-B-1
     *              |-> PART-009-A-1
     *      |-> PART-008-B-1
     *              |-> PART-013-A-1
     *                     |--> PART-016-A-1
     *                             |-->PART-018-A-1
     *                                     |--> PART-019-A-1
     *                                             |--> PART-020-A-1
     *              |-> PART-020-A-1
     *              |-> PART-019-A-1
     *                     |-> PART-020-A-1
     *
     */
    public static void buildBasicStructure(){

        //No released and only last revision checked out

        //Set the root part members
        PartMaster partMaster = getPartMasterWith("PART-001");

        //addRevisionWithPartLinkTo(partMaster, membersRevisionX, released <=> true, checkout <=> true )
        String[] membersRevisionB = {"PART-002","PART-006","PART-018"};
        String[] membersRevisionC = {"PART-002","PART-006","PART-015"};
        String[] membersRevisionD = {"PART-002","PART-006","PART-015"};
        String[] membersRevisionE = {"PART-002","PART-006","PART-007","PART-008"};

        //Set parts master's members
        PartMaster part002 = getPartMasterWith("PART-002");
        String[] membersRevisionB002 = {"PART-009","PART-010","PART-011"};
        addRevisionWithPartLinkTo(part002, membersRevisionB002, false, false);

        PartMaster part006 = getPartMasterWith("PART-006");
        String[] membersRevisionB006 = {"PART-012","PART-002","PART-004"};
        addRevisionWithPartLinkTo(part006, membersRevisionB006, false, false);

        PartMaster part018 = getPartMasterWith("PART-018");
        String[] membersRevisionB018 = {"PART-003","PART-014"};
        addRevisionWithPartLinkTo(part018, membersRevisionB018, false, false);

        PartMaster part015 =  getPartMasterWith("PART-015");
        String[] membersRevisionB015 = {"PART-007","PART-008","PART-004"};
        addRevisionWithPartLinkTo(part015, membersRevisionB015, false, false);

        PartMaster part007 = getPartMasterWith("PART-007");
        String[] membersRevisionB007 = {"PART-009"};
        addRevisionWithPartLinkTo(part007, membersRevisionB007, false, false);

        PartMaster part008 =  getPartMasterWith("PART-008");
        String[] membersRevisionB008 = {"PART-013","PART-020","PART-019"};
        addRevisionWithPartLinkTo(part008, membersRevisionB008, false, false);

        //Add revision to part master
        addRevisionWithPartLinkTo(partMaster, membersRevisionB, false, false);
        addRevisionWithPartLinkTo(partMaster, membersRevisionC, false, false);
        addRevisionWithPartLinkTo(partMaster, membersRevisionD, false, false);
        addRevisionWithPartLinkTo(partMaster, membersRevisionE, false, true);//CheckedOut revision

        //Add substitutes to some parts ( optional parts )
        String[] substitutes013 = {"PART-007"};
        String[] substitutes004 = {"PART-010"};
        String[] substitutes011 = {"PART-016","PART-012","PART-008"};

        addSubstituteInLastIterationOfLastRevisionTo(part008, substitutes013 , "PART-013");
        addSubstituteInLastIterationOfLastRevisionTo(part015, substitutes004 , "PART-004");
        addSubstituteInLastIterationOfLastRevisionTo(part002, substitutes011 , "PART-011");
    }

    /**
     *
     * Map structure
     *
     *     "PartMaster" => root part ( PartMaster )
     *     "Revisions" => HashMap<String, List<Revision>>
     *     "Iterations" => HashMap<Integer, List<Iteration>
     */
    private static void createMetaData(int partCountNumber){

        if(partCountNumber == 0 ){

            return;
        }
        PartMaster tmpPart = existingPart_list.get(partCountNumber - 1);
        HashMap<String,PartMaster> tmpDateMap = new HashMap<>();
        tmpDateMap.put(PARTMASTER_MAP_KEY,tmpPart);
        List<PartIteration> iteration_list = new ArrayList<>();
        for(PartRevision revision : tmpPart.getPartRevisions()) {

            iteration_list.addAll(revision.getPartIterations());
        }
        setPartRevisionsOf(tmpPart.getNumber(), tmpPart.getPartRevisions());
        setPartIterationsOf(tmpPart.getNumber(), iteration_list);
        metaData_list_master.put(tmpPart.getNumber(),tmpDateMap);
        createMetaData(partCountNumber-1);
    }

    private static boolean exist(int iteration,String partNumber) {

        for (PartIteration partIteration : getPartIterationsOf(partNumber)){

            if (iteration == partIteration.getIteration()) {

                return true;
            }
        }
        return false;
    }

    private static PartRevision getRevisionWith(String version,String partNumber,int lastIndex){

        if(lastIndex == 0){

            return null;
        }
        PartRevision result = metaData_list_revision.get(partNumber).get(REVISIONS_MAP_KEY).get(lastIndex - 1);
        if(version.equals(result.getVersion())){

            return  result;
        }
        return getRevisionWith(version,partNumber,lastIndex-1);
    }

    private static void sort(List<PartMaster> list, int size)
    {

        if (size == 1) {

            return;
        }
        for (int i=0; i<size-1; i++) {

            if (STRING_COMPARATOR.compare(list.get(i).getNumber(), list.get(i + 1).getNumber()) > 0) {

                PartMaster temp = list.get(i);
                list.set(i, list.get(i + 1));
                list.set(i + 1, temp);
            }
        }
        sort(list, size-1);
    }
    private static PartMaster buildPartMaster(String number) throws Exception {

        PartMaster tmpPart = new PartMaster(workspace,number,user);
        PartRevision tmpRevision = tmpPart.createNextRevision(user);
        if (!rootsPartNumberInBuild.contains(number)) {

            rootsPartNumberInBuild.add(number);
        }
        PartIteration tmpIteration = buildIteration(defaultMappingUsageLink_tab[Integer.parseInt(number.split("-")[1]) - 1], tmpRevision);
        for(PartUsageLink link : tmpIteration.getComponents()){

            if(!existingPart_list.contains(link.getComponent())){

                existingPart_list.add(link.getComponent());
            }
        }
        createdNumber_list.add(number);
        return tmpPart;
    }

    private static PartIteration buildIteration(int[] related, PartRevision revision) throws Exception {

        PartIteration iteration = revision.createNextIteration(user);
        iteration.setComponents(buildUsageLink(revision.getPartNumber(), related));
        return iteration;
    }

    private static List<PartUsageLink> buildUsageLink(String revisionPartNumber, int[] related ) throws Exception {

        List<PartUsageLink> result =  new ArrayList<>();

        for(int pointer : related) {

            PartUsageLink partUsageLink = new PartUsageLink();
            partUsageLink.setOptional(false);
            partUsageLink.setAmount(1);
            if(rootsPartNumberInBuild.contains(defaultPartsNumber_list[pointer])){

                throw new Exception("loop detected "+revisionPartNumber+" => "+defaultPartsNumber_list[pointer]);
            }
            PartMaster component = buildPartMaster(defaultPartsNumber_list[pointer]);
            partUsageLink.setReferenceDescription(component.getNumber() + "-UsageLink");
            partUsageLink.setComponent(component);
            result.add(partUsageLink);
        }
        return result;
    }
    //END : DEFAULT PARTS
}