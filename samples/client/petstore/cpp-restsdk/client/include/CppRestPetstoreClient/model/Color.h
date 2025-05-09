/**
 * OpenAPI Petstore
 * This is a sample server Petstore server. For this sample, you can use the api key `special-key` to test the authorization filters.
 *
 * The version of the OpenAPI document: 1.0.0
 *
 * NOTE: This class is auto generated by OpenAPI-Generator 7.14.0-SNAPSHOT.
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

/*
 * Color.h
 *
 * pet color in the store
 */

#ifndef ORG_OPENAPITOOLS_CLIENT_MODEL_Color_H_
#define ORG_OPENAPITOOLS_CLIENT_MODEL_Color_H_


#include "CppRestPetstoreClient/ModelBase.h"


namespace org {
namespace openapitools {
namespace client {
namespace model {


class  Color
    : public ModelBase
{
public:
    Color();
    Color(utility::string_t str);
    operator utility::string_t() const {
        return enumToStrMap.at(getValue());
    }


    virtual ~Color();

    /////////////////////////////////////////////
    /// ModelBase overrides

    void validate() override;

    web::json::value toJson() const override;
    bool fromJson(const web::json::value& json) override;

    void toMultipart(std::shared_ptr<MultipartFormData> multipart, const utility::string_t& namePrefix) const override;
    bool fromMultiPart(std::shared_ptr<MultipartFormData> multipart, const utility::string_t& namePrefix) override;

    enum class eColor
    {
        Color_BLACK,
        Color_WHITE,
        Color_BROWN,
        Color_GOLDEN,
        Color_MIXED,
    };

    eColor getValue() const;
    void setValue(eColor const value);

    protected:
        eColor m_value;
        std::map<eColor,utility::string_t> enumToStrMap = {
        { eColor::Color_BLACK, "BLACK" },
        { eColor::Color_WHITE, "WHITE" },
        { eColor::Color_BROWN, "BROWN" },
        { eColor::Color_GOLDEN, "GOLDEN" },
        { eColor::Color_MIXED, "MIXED" }
};
        std::map<utility::string_t,eColor> strToEnumMap = {
        { "BLACK", eColor::Color_BLACK },
        { "WHITE", eColor::Color_WHITE },
        { "BROWN", eColor::Color_BROWN },
        { "GOLDEN", eColor::Color_GOLDEN },
        { "MIXED", eColor::Color_MIXED }
};

};

}
}
}
}

#endif /* ORG_OPENAPITOOLS_CLIENT_MODEL_Color_H_ */
