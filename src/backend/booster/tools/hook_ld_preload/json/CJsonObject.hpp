/*******************************************************************************
 * Project:  neb
 * @file     CJsonObject.hpp
 * @brief    Json
 * @author   bwarliao
 * @date:    2014-7-16
 * @note
 * Modify history:
 ******************************************************************************/

#ifndef CJSONOBJECT_HPP_
#define CJSONOBJECT_HPP_

#include <stdio.h>
#include <stddef.h>

#if defined(__MACH__)
#include <stdlib.h>
#else
#include <malloc.h>
#endif

#include <errno.h>
#include <limits.h>
#include <math.h>
#include <string>
#include <map>
#include <list>
#ifdef __cplusplus
extern "C"
{
#endif
#include "cJSON.h"
#ifdef __cplusplus
}
#endif

namespace neb
{

    class CJsonObject
    {
    public: // method of ordinary json object or json array
        CJsonObject();
        CJsonObject(const std::string &strJson);
        CJsonObject(const CJsonObject *pJsonObject);
        CJsonObject(const CJsonObject &oJsonObject);
        virtual ~CJsonObject();

        CJsonObject &operator=(const CJsonObject &oJsonObject);
        bool operator==(const CJsonObject &oJsonObject) const;
        bool Parse(const std::string &strJson);
        void Clear();
        bool IsEmpty() const;
        bool IsArray() const;
        std::string ToString() const;
        std::string ToFormattedString() const;
        const std::string &GetErrMsg() const
        {
            return (m_strErrMsg);
        }

    public: // method of ordinary json object
        bool AddEmptySubObject(const std::string &strKey);
        bool AddEmptySubArray(const std::string &strKey);
        bool GetKey(std::string &strKey);
        void ResetTraversing();
        CJsonObject &operator[](const std::string &strKey);
        std::string operator()(const std::string &strKey) const;
        bool Get(const std::string &strKey, CJsonObject &oJsonObject) const;
        bool Get(const std::string &strKey, std::string &strValue) const;
        bool Get(const std::string &strKey, int32 &iValue) const;
        bool Get(const std::string &strKey, uint32 &uiValue) const;
        bool Get(const std::string &strKey, int64 &llValue) const;
        bool Get(const std::string &strKey, uint64 &ullValue) const;
        bool Get(const std::string &strKey, bool &bValue) const;
        bool Get(const std::string &strKey, float &fValue) const;
        bool Get(const std::string &strKey, double &dValue) const;
        bool IsNull(const std::string &strKey) const;
        bool Add(const std::string &strKey, const CJsonObject &oJsonObject);
        bool Add(const std::string &strKey, const std::string &strValue);
        bool Add(const std::string &strKey, int32 iValue);
        bool Add(const std::string &strKey, uint32 uiValue);
        bool Add(const std::string &strKey, int64 llValue);
        bool Add(const std::string &strKey, uint64 ullValue);
        bool Add(const std::string &strKey, bool bValue, bool bValueAgain);
        bool Add(const std::string &strKey, float fValue);
        bool Add(const std::string &strKey, double dValue);
        bool AddNull(const std::string &strKey); // add null like this:   "key":null
        bool Delete(const std::string &strKey);
        bool Replace(const std::string &strKey, const CJsonObject &oJsonObject);
        bool Replace(const std::string &strKey, const std::string &strValue);
        bool Replace(const std::string &strKey, int32 iValue);
        bool Replace(const std::string &strKey, uint32 uiValue);
        bool Replace(const std::string &strKey, int64 llValue);
        bool Replace(const std::string &strKey, uint64 ullValue);
        bool Replace(const std::string &strKey, bool bValue, bool bValueAgain);
        bool Replace(const std::string &strKey, float fValue);
        bool Replace(const std::string &strKey, double dValue);
        bool ReplaceWithNull(const std::string &strKey); // replace value with null

    public: // method of json array
        int GetArraySize();
        CJsonObject &operator[](unsigned int uiWhich);
        std::string operator()(unsigned int uiWhich) const;
        bool Get(int iWhich, CJsonObject &oJsonObject) const;
        bool Get(int iWhich, std::string &strValue) const;
        bool Get(int iWhich, int32 &iValue) const;
        bool Get(int iWhich, uint32 &uiValue) const;
        bool Get(int iWhich, int64 &llValue) const;
        bool Get(int iWhich, uint64 &ullValue) const;
        bool Get(int iWhich, bool &bValue) const;
        bool Get(int iWhich, float &fValue) const;
        bool Get(int iWhich, double &dValue) const;
        bool IsNull(int iWhich) const;
        bool Add(const CJsonObject &oJsonObject);
        bool Add(const std::string &strValue);
        bool Add(int32 iValue);
        bool Add(uint32 uiValue);
        bool Add(int64 llValue);
        bool Add(uint64 ullValue);
        bool Add(int iAnywhere, bool bValue);
        bool Add(float fValue);
        bool Add(double dValue);
        bool AddNull(); // add a null value
        bool AddAsFirst(const CJsonObject &oJsonObject);
        bool AddAsFirst(const std::string &strValue);
        bool AddAsFirst(int32 iValue);
        bool AddAsFirst(uint32 uiValue);
        bool AddAsFirst(int64 llValue);
        bool AddAsFirst(uint64 ullValue);
        bool AddAsFirst(int iAnywhere, bool bValue);
        bool AddAsFirst(float fValue);
        bool AddAsFirst(double dValue);
        bool AddNullAsFirst(); // add a null value
        bool Delete(int iWhich);
        bool Replace(int iWhich, const CJsonObject &oJsonObject);
        bool Replace(int iWhich, const std::string &strValue);
        bool Replace(int iWhich, int32 iValue);
        bool Replace(int iWhich, uint32 uiValue);
        bool Replace(int iWhich, int64 llValue);
        bool Replace(int iWhich, uint64 ullValue);
        bool Replace(int iWhich, bool bValue, bool bValueAgain);
        bool Replace(int iWhich, float fValue);
        bool Replace(int iWhich, double dValue);
        bool ReplaceWithNull(int iWhich); // replace with a null value

    private:
        CJsonObject(cJSON *pJsonData);

    private:
        cJSON *m_pJsonData;
        cJSON *m_pExternJsonDataRef;
        cJSON *m_pKeyTravers;
        std::string m_strErrMsg;
        std::map<unsigned int, CJsonObject *> m_mapJsonArrayRef;
        std::map<std::string, CJsonObject *> m_mapJsonObjectRef;
    };

} // namespace neb

#endif /* CJSONHELPER_HPP_ */
